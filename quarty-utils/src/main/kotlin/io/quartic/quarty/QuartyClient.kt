package io.quartic.quarty

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import okhttp3.ResponseBody
import java.net.URI
import java.time.Clock
import java.util.concurrent.CompletableFuture

// TODO: Fix this
typealias QuartyErrorDetail = Any

class QuartyClient(
    private val quarty: Quarty,
    private val clock: Clock
) {
    private val LOG by logger()

    constructor(
        clientBuilder: ClientBuilder,
        url: String,
        clock: Clock = Clock.systemUTC()
    ) : this(clientBuilder.retrofit<Quarty>(url, timeoutSeconds = 60 * 60), clock)

    fun initAsync(repoUrl: URI, repoCommit: String): CompletableFuture<out QuartyResult<Unit>> =
        invokeAsync { initAsync(repoUrl, repoCommit) }

    fun evaluateAsync(): CompletableFuture<out QuartyResult<Pipeline>> =
        invokeAsync { evaluateAsync() }

    fun executeAsync(step: String, namespace: String): CompletableFuture<out QuartyResult<Unit>> =
        invokeAsync { executeAsync(step, namespace) }

    private inline fun <reified R : Any> invokeAsync(
        block: Quarty.() -> CompletableFuture<ResponseBody>
    ): CompletableFuture<QuartyResult<R>> =
        block(quarty)
            .thenApply { responseBody ->
                val logEvents = mutableListOf<LogEvent>()
                var finaliser: () -> QuartyResult<R> = { InternalError(logEvents, "No terminating message received") }

                try {
                    responseBody.use {
                        it.byteStream()
                            .bufferedReader()
                            .lines()
                            .filter { !it.isEmpty() }
                            .map { OBJECT_MAPPER.readValue<QuartyMessage>(it) }
                            .forEach {
                                when (it) {
                                    is QuartyMessage.Log ->
                                        logEvents += LogEvent(it.stream, it.line, clock.instant())
                                    is QuartyMessage.Progress ->
                                        logEvents += LogEvent("progress", it.message, clock.instant())
                                    is QuartyMessage.Result -> {
                                        val result = when (R::class) {
                                            Unit::class -> Unit as R
                                            else -> OBJECT_MAPPER.convertValue(it.result, R::class.java)    // For impenetrable reasons, Kotlin convertValue<> extension doesn't work properly
                                        }
                                        finaliser = { Success(logEvents, result) }
                                    }
                                    is QuartyMessage.Error ->
                                        finaliser = { Failure(logEvents, it.detail) }
                                }
                            }
                    }
                } catch (e: Exception) {
                    LOG.error("Error invoking Quarty", e)
                    finaliser = { InternalError(logEvents, "Error invoking Quarty") }
                }

                finaliser()
            }
}

