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

class QuartyClient(val quarty: Quarty) {
    sealed class QuartyResult {
        data class Success(val log: String, val dag: List<Step>) : QuartyResult()
        data class Failure(val log: String) : QuartyResult()
    }

    constructor(clientBuilder: ClientBuilder, url: String) :
        this(clientBuilder.retrofit<Quarty>(url))

    fun stream(repoUrl: URI, repoCommit: String): CompletableFuture<Stream<QuartyMessage>> = quarty
        .getPipeline(repoUrl, repoCommit)
        .thenApply { responseBody ->
            responseBody.byteStream()
                .bufferedReader()
                .lines()
                .filter { !it.isEmpty() }
                .map { OBJECT_MAPPER.readValue<QuartyMessage>(it) }
        }

    fun getResult(repoUrl: URI, repoCommit: String): CompletableFuture<out QuartyResult?> = stream(repoUrl, repoCommit)
        .thenApply { stream ->
            val messages = stream.toList()
            val log = messages.filter { message -> message is QuartyMessage.Log }
                .map { message -> (message as QuartyMessage.Log).line }
                .joinToString("\n")

            messages.map { message ->
                when (message) {
                    is QuartyMessage.Result -> QuartyResult.Success(log, message.result)
                    is QuartyMessage.Error -> QuartyResult.Failure(log)
                    else -> null
                }
            }.filterNotNull().first()
        }
}
