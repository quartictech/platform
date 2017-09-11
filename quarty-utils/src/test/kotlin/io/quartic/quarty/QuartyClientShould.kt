package io.quartic.quarty

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.*
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyResponse.*
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import io.quartic.quarty.model.Step
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture.completedFuture

class QuartyClientShould {
    private val clock = mock<Clock>()
    private val quarty = mock<Quarty>()
    private val client = QuartyClient(quarty, clock)

    private val instantA = mock<Instant>()
    private val instantB = mock<Instant>()

    private lateinit var responseBody: ResponseBody

    @Before
    fun before() {
        whenever(clock.instant())
            .thenReturn(instantA)
            .thenReturn(instantB)
    }

    @Test
    fun return_success_if_quarty_returns_result() {
        val pipeline = Pipeline(listOf(
            Step("123", "Foo", "Bar", "file/yeah.py", listOf(1, 2), emptyList(), emptyList())
        ))

        quartyWillSend(listOf(
            Result(OBJECT_MAPPER.convertValue(pipeline))
        ))

        assertThat(invokeQuarty(), equalTo(Success(
            emptyList(),
            pipeline
        ) as QuartyResult<*>))
    }

    @Test
    fun return_failure_if_quarty_returns_error() {
        quartyWillSend(listOf(
            Error("Big problems")
        ))

        assertThat(invokeQuarty(), equalTo(Failure<Any>(
            emptyList(),
            "Big problems"
        ) as QuartyResult<*>))
    }

    @Test
    fun include_other_messages_in_result() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            Progress("Lovely time"),
            Error("Big problems")
        ))

        assertThat(invokeQuarty(), equalTo(Failure<Any>(
            listOf(
                LogEvent("stdout", "Yeah", instantA),
                LogEvent("progress", "Lovely time", instantB)
            ),
            "Big problems"
        ) as QuartyResult<*>))
    }

    @Test
    fun return_internal_error_if_no_result_or_failure() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            Progress("Lovely time")
            // No result or error here!
        ))

        assertThat(invokeQuarty(), equalTo(InternalError<Any>(
            listOf(
                LogEvent("stdout", "Yeah", instantA),
                LogEvent("progress", "Lovely time", instantB)
            ),
            "No terminating message received"
        ) as QuartyResult<*>))
    }

    @Test
    fun ignore_payload_for_success_messages_if_type_is_unit() {
        quartyWillSend(listOf(
            mapOf("type" to "result", "result" to null)
        ))

        assertThat(
            client.initAsync(mock(), "yeah").get(),
            equalTo(Success(emptyList(), Unit) as QuartyResult<*>)
        )
    }

    @Test
    fun return_internal_error_if_parsing_fails() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            mapOf("type" to "gibberish"),
            Log("stdout", "Oh yeah")
        ))

        assertThat(invokeQuarty(), equalTo(InternalError<Any>(
            listOf(
                LogEvent("stdout", "Yeah", instantA)
                // Second log message not captured
            ),
            "Error invoking Quarty"
        ) as QuartyResult<*>))
    }

    @Test
    fun close_response_body_when_completing_normally() {
        quartyWillSend(listOf(
            Error("Oh dear")
        ))

        invokeQuarty()

        verify(responseBody).close()
    }

    @Test
    fun close_response_body_when_something_bad_happened() {
        quartyWillSend(listOf(
            mapOf("type" to "gibberish")
        ))

        invokeQuarty()

        verify(responseBody).close()
    }

    private fun invokeQuarty() = client.evaluateAsync().get()

    private fun quartyWillSend(messages: List<Any>) {
        responseBody = spy(ResponseBody.create(
            MediaType.parse("application/x-ndjson"),
            messages.toNdJson()
        ))

        whenever(quarty.initAsync(any(), any())).thenReturn(completedFuture(responseBody))
        whenever(quarty.evaluateAsync()).thenReturn(completedFuture(responseBody))
    }

    private fun List<Any>.toNdJson() = map(OBJECT_MAPPER::writeValueAsString).joinToString("\n")
}
