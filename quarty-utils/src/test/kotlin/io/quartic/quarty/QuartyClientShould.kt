package io.quartic.quarty

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.QuartyClient.QuartyResult
import io.quartic.quarty.QuartyClient.QuartyResult.Failure
import io.quartic.quarty.QuartyClient.QuartyResult.Success
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.util.concurrent.CompletableFuture.completedFuture

class QuartyClientShould {
    private val quarty = mock<Quarty>()
    private val client = QuartyClient(quarty)

    private val repoUrl = URI("http://noob.com")
    private val repoCommit = "1234"

    @Test
    fun return_success_if_quarty_returns_result() {
        val steps = listOf(
            Step("123", "Foo", "Bar", "file/yeah.py", listOf(1, 2), emptyList(), emptyList())
        )

        val messages = listOf(
            QuartyMessage.Result(steps)
        )

        quartyWillSend(messages)

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Success(messages, steps) as QuartyResult))
    }

    @Test
    fun return_failure_if_quarty_returns_error() {
        val messages = listOf(
            QuartyMessage.Error("Big problems")
        )

        quartyWillSend(messages)

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Failure(messages, "Big problems") as QuartyResult))
    }

    @Test
    fun include_other_messages_in_result() {
        val messages = listOf(
            QuartyMessage.Log("stdout", "Yeah"),
            QuartyMessage.Progress("Lovely time"),
            QuartyMessage.Error("Big problems")
        )

        quartyWillSend(messages)

        val result = client.getResultAsync(repoUrl, repoCommit).get()

        assertThat(result, equalTo(Failure(messages, "Big problems") as QuartyResult))
    }

    @Test
    fun return_null_if_no_result_or_error() {
        val messages = listOf(
            QuartyMessage.Log("stdout", "Yeah"),
            QuartyMessage.Progress("Lovely time")
            // No result or error here!
        )

        quartyWillSend(messages)

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
