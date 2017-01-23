package io.quartic.tracker.scribe

import com.google.cloud.pubsub.PubSubException
import com.google.cloud.pubsub.ReceivedMessage
import com.google.cloud.pubsub.Subscription
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Test
import java.io.IOException

class MessageExtractorShould {
    private val subscription = mock<Subscription>()
    private val handler = mock<(List<String>) -> Boolean>()
    private val extractor = MessageExtractor(subscription, handler, BATCH_SIZE)

    @Test
    fun pull_and_pass_results_to_handler() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockHandlerFailure()

        extractor.run()

        verify(subscription).pull(BATCH_SIZE)
        verify(handler).invoke(listOf("foo", "bar"))
    }

    @Test
    fun ack_all_messages_if_handler_succeeds() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockHandlerSuccess()

        extractor.run()

        messages.forEach { verify(it).ack() }
    }

    @Test
    fun ack_no_messages_if_handler_fails() {
        val messages = listOf(message("foo"), message("bar"))
        mockAvailableMessages(messages)
        mockHandlerFailure()

        extractor.run()

        messages.forEach { verify(it, never()).ack() }
    }

    @Test
    fun not_call_handler_if_nothing_to_process() {
        mockAvailableMessages(emptyList())

        extractor.run()

        verify(handler, never()).invoke(any())
    }

    @Test
    fun pull_again_if_first_pull_returned_max_results() {
        mockAvailableMessages(listOf(message("foo"), message("bar"), message("baz")))     // Equal to BATCH_SIZE
        mockHandlerSuccess()

        extractor.run()

        verify(subscription, times(2)).pull(BATCH_SIZE)
    }

    @Test
    fun not_pull_again_if_first_pull_returned_max_results_but_handler_failed() {
        mockAvailableMessages(listOf(message("foo"), message("bar"), message("baz")))        // Equal to BATCH_SIZE
        mockHandlerFailure()

        extractor.run()

        verify(subscription, times(1)).pull(BATCH_SIZE)
    }

    @Test
    fun not_pull_again_if_first_pull_returned_less_than_max_results() {
        mockAvailableMessages(listOf(message("foo"), message("bar")))     // Less than BATCH_SIZE
        mockHandlerSuccess()

        extractor.run()

        verify(subscription, times(1)).pull(BATCH_SIZE)
    }

    @Test
    fun not_throw_if_subscription_fails() {
        whenever(subscription.pull(any())).thenThrow(PubSubException(IOException("Bad"), true))

        extractor.run()
    }

    private fun mockAvailableMessages(messages: List<ReceivedMessage>) {
        whenever(subscription.pull(any())).thenReturn(messages.iterator())
    }

    private fun mockHandlerSuccess() {
        whenever(handler.invoke(any())).thenReturn(true)
    }

    private fun mockHandlerFailure() {
        whenever(handler.invoke(any())).thenReturn(false)
    }

    private fun message(payload: String) = mock<ReceivedMessage> {
        on { payloadAsString } doReturn payload
    }

    companion object {
        private val BATCH_SIZE = 3
    }
}