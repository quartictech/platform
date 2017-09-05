package io.quartic.quarty

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyMessage.*
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import io.quartic.quarty.model.Step
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
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
    fun return_null_if_no_result_or_error() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            Progress("Lovely time")
            // No result or error here!
        ))

        assertThat(invokeQuarty(), nullValue())
    }

    @Test
    fun allow_null_payloads_for_success_messages() {
        quartyWillSend(listOf(
            mapOf("type" to "result", "result" to null)
        ))

        invokeQuarty()
        // No error
    }

    private fun invokeQuarty() = client.invokeAsync<Pipeline> { evaluateAsync() }.get()

    private fun quartyWillSend(messages: List<Any>) {
        whenever(quarty.evaluateAsync()).thenReturn(completedFuture(
            ResponseBody.create(
                MediaType.parse("application/x-ndjson"),
                messages.toNdJson()
            )
        ))
    }

    private fun List<Any>.toNdJson() = map(OBJECT_MAPPER::writeValueAsString).joinToString("\n")
}
