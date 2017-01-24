package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubException
import com.google.cloud.pubsub.ReceivedMessage
import com.google.cloud.pubsub.Subscription
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class MessageExtractorShould {
    private val subscription = mock<Subscription>()
    private val clock = TickingClock(Instant.EPOCH)
    private val writer = mock<BatchWriter>()
    private val extractor = MessageExtractor(subscription, clock, writer, BATCH_SIZE)

    @Test
    fun pull_and_pass_results_to_handler() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockWriterFailure()

        extractor.run()

        verify(subscription).pull(BATCH_SIZE)
        verify(writer).write(listOf("foo", "bar"), Instant.EPOCH, 0)
    }

    @Test
    fun ack_all_messages_if_handler_succeeds() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockWriterSuccess()

        extractor.run()

        messages.forEach { verify(it).ack() }
    }

    @Test
    fun ack_no_messages_if_handler_fails() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockWriterFailure()

        extractor.run()

        messages.forEach { verify(it, never()).ack() }
    }

    @Test
    fun not_call_handler_if_nothing_to_process() {
        mockAvailableMessages(emptyList())

        extractor.run()

        verify(writer, never()).write(any(), any(), any())
    }

    @Test
    fun always_grab_the_latest_timestamp_but_start_with_part_number_0() {
        mockAvailableMessages(
                listOf(message("foo"), message("bar")),
                listOf(message("apple"), message("banana"))
        )
        mockWriterSuccess()

        extractor.run()
        extractor.run()

        verify(writer).write(any(), eq(Instant.EPOCH), eq(0))
        verify(writer).write(any(), eq(Instant.EPOCH.plusSeconds(1)), eq(0))
    }

    @Test
    fun pull_again_and_increment_part_number_but_maintain_same_timestamp_if_first_pull_returned_max_results() {
        mockAvailableMessages(
                listOf(message("foo"), message("bar"), message("baz")),                      // Equal to BATCH_SIZE
                listOf(message("apple"), message("banana"))
        )
        mockWriterSuccess()

        extractor.run()

        verify(subscription, times(2)).pull(BATCH_SIZE)
        verify(writer).write(any(), eq(Instant.EPOCH), eq(0))
        verify(writer).write(any(), eq(Instant.EPOCH), eq(1))   // Underlying clock is ticking, so this check guarantees we're holding the first value
    }

    @Test
    fun not_pull_again_if_first_pull_returned_max_results_but_handler_failed() {
        mockAvailableMessages(listOf(message("foo"), message("bar"), message("baz")))        // Equal to BATCH_SIZE
        mockWriterFailure()

        extractor.run()

        verify(subscription, times(1)).pull(BATCH_SIZE)
    }

    @Test
    fun not_pull_again_if_first_pull_returned_less_than_max_results() {
        mockAvailableMessages(listOf(message("foo"), message("bar")))     // Less than BATCH_SIZE
        mockWriterSuccess()

        extractor.run()

        verify(subscription, times(1)).pull(BATCH_SIZE)
    }

    @Test
    fun not_throw_if_subscription_fails() {
        whenever(subscription.pull(any())).thenThrow(PubSubException(IOException("Bad"), true))

        extractor.run()
    }

    private fun mockAvailableMessages(vararg messages: List<ReceivedMessage>) {
        var stubbing = whenever(subscription.pull(any()))
        messages.forEach { stubbing = stubbing.thenReturn(it.iterator()) }
    }

    private fun mockWriterSuccess() {
        whenever(writer.write(any(), any(), any())).thenReturn(true)
    }

    private fun mockWriterFailure() {
        whenever(writer.write(any(), any(), any())).thenReturn(false)
    }

    private fun message(payload: String) = mock<ReceivedMessage> {
        on { payloadAsString } doReturn payload
    }

    private class TickingClock(private val base: Instant) : Clock() {
        private var i = 0L

        override fun instant() = base + Duration.ofSeconds(i++)

        override fun withZone(zone: ZoneId?) = throw UnsupportedOperationException("not implemented")

        override fun getZone() = throw UnsupportedOperationException("not implemented")
    }

    companion object {
        private val BATCH_SIZE = 3
    }
}