package io.quartic.quarty

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.client.ClientBuilder
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import java.net.URI
import java.time.Clock
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream

// TODO: Fix this
typealias QuartyErrorDetail = Any

class QuartyClient(
    private val quarty: Quarty,
    private val clock: Clock
) {

    constructor(
        clientBuilder: ClientBuilder,
        url: String,
        clock: Clock = Clock.systemUTC()
    ) : this(clientBuilder.retrofit<Quarty>(url, timeoutSeconds = 300), clock)

    fun getResultAsync(repoUrl: URI, repoCommit: String): CompletableFuture<out QuartyResult?> = stream(repoUrl, repoCommit)
        .thenApply { stream ->
            val logEvents = mutableListOf<LogEvent>()
            var finaliser: () -> QuartyResult? = { null }

            stream.forEach {
                when (it) {
                    is QuartyMessage.Log ->
                        logEvents += LogEvent(it.stream, it.line, clock.instant())
                    is QuartyMessage.Progress ->
                        logEvents += LogEvent("progress", it.message, clock.instant())
                    is QuartyMessage.Result ->
                        finaliser = { Success(logEvents, it.result) }
                    is QuartyMessage.Error ->
                        finaliser = { Failure(logEvents, it.detail) }
                }
            }

            finaliser()
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
