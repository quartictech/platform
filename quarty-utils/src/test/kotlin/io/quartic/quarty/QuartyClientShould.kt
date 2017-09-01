package io.quartic.quarty

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.QuartyMessage
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
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture.completedFuture

class QuartyClientShould {
    private val clock = mock<Clock>()
    private val quarty = mock<Quarty>()
    private val client = QuartyClient(quarty, clock)

    private val repoUrl = URI("http://noob.com")
    private val repoCommit = "1234"

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
        val steps = listOf(
            Step("123", "Foo", "Bar", "file/yeah.py", listOf(1, 2), emptyList(), emptyList())
        )

        quartyWillSend(listOf(
            Result(steps)
        ))

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Success(
            emptyList(),
            steps
        ) as QuartyResult))
    }

    @Test
    fun return_failure_if_quarty_returns_error() {
        quartyWillSend(listOf(
            Error("Big problems")
        ))

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Failure(
            emptyList(),
            "Big problems"
        ) as QuartyResult))
    }

    @Test
    fun include_other_messages_in_result() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            Progress("Lovely time"),
            Error("Big problems")
        ))

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Failure(
            listOf(
                LogEvent("stdout", "Yeah", instantA),
                LogEvent("progress", "Lovely time", instantB)
            ),
            "Big problems"
        ) as QuartyResult))
    }

    @Test
    fun return_null_if_no_result_or_error() {
        quartyWillSend(listOf(
            Log("stdout", "Yeah"),
            Progress("Lovely time")
            // No result or error here!
        ))

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, nullValue())
    }


    private fun quartyWillSend(messages: List<QuartyMessage>) {
        whenever(quarty.getPipeline(repoUrl, repoCommit)).thenReturn(completedFuture(
            ResponseBody.create(
                MediaType.parse("application/x-ndjson"),
                messages.toNdJson()
            )
        ))
    }

    private fun List<QuartyMessage>.toNdJson() = map(OBJECT_MAPPER::writeValueAsString).joinToString("\n")
}
