package io.quartic.quarty

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.client.ClientBuilder
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.streams.toList

// TODO: Fix this
typealias QuartyErrorDetail = Any

class QuartyClient(private val quarty: Quarty) {
    sealed class QuartyResult {
        abstract val messages: List<QuartyMessage>

        data class Success(override val messages: List<QuartyMessage>, val result: List<Step>): QuartyResult()
        data class Failure(override val messages: List<QuartyMessage>, val detail: QuartyErrorDetail?) : QuartyResult()
    }

    constructor(clientBuilder: ClientBuilder, url: String) :
        this(clientBuilder.retrofit<Quarty>(url, timeoutSeconds = 300))

    fun getResultAsync(repoUrl: URI, repoCommit: String): CompletableFuture<QuartyResult?> = stream(repoUrl, repoCommit)
        .thenApply { stream ->
            val messages = stream.toList()

            messages.mapNotNull { message ->
                when (message) {
                    is QuartyMessage.Result -> QuartyResult.Success(messages, message.result)
                    is QuartyMessage.Error -> QuartyResult.Failure(messages, message.detail)
                    else -> null
                }
            }.firstOrNull()
        }

    private fun stream(repoUrl: URI, repoCommit: String): CompletableFuture<Stream<QuartyMessage>> = quarty
        .getPipeline(repoUrl, repoCommit)
        .thenApply { responseBody ->
            responseBody.byteStream()
                .bufferedReader()
                .lines()
                .filter { !it.isEmpty() }
                .map { OBJECT_MAPPER.readValue<QuartyMessage>(it) }
        }
}
