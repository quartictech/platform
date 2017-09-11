@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package io.quartic.eval.quarty

import com.google.common.base.Throwables.getRootCause
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.QuartyMessage.Progress
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch
import java.net.URI
import java.time.Clock
import kotlin.reflect.KClass

class QuartyProxy<R : Any>(
    private val quarty: WebsocketClient<Unit, QuartyMessage>,
    private val clock: Clock,
    private val clazz: KClass<R>
) : AutoCloseable {
    private val LOG by logger()

    private var complete = false
    private val _results = Channel<QuartyResult<R>>(Channel.UNLIMITED)
    val results: ReceiveChannel<QuartyResult<R>> = _results
    private val logEvents = mutableListOf<LogEvent>()
    private val job = launch(CommonPool) {
        try {
            runEventLoop()
        } finally {
            quarty.close()
            _results.close()
        }
    }

    override fun close() {
        job.cancel()
    }

    private suspend fun runEventLoop() {
        while (!complete) {
            val event = quarty.events.receive()
            when (event) {
                is Connected -> {}
                is Disconnected -> { handleDisconnected() }
                is MessageReceived -> { handleMessageReceived(event.message) }
            }
        }
    }

    private suspend fun handleDisconnected() {
        sendResult(InternalError(logEvents, "No terminating message received"))
    }

    private suspend fun handleMessageReceived(message: QuartyMessage) {
        when (message) {
            is Progress ->
                logEvents += LogEvent("progress", message.message, clock.instant())
            is QuartyMessage.Log ->
                logEvents += LogEvent(message.stream, message.line, clock.instant())
            is QuartyMessage.Result -> {
                sendResult(try {
                    Success(logEvents, convertResult(message))
                } catch (e: Exception) {
                    LOG.error("Error parsing result", getRootCause(e))
                    InternalError<R>(logEvents, "Error parsing reuslt from Quarty")
                })
            }
            is QuartyMessage.Error ->
                sendResult(Failure(logEvents, message.detail))
        }
    }

    private suspend fun sendResult(result: QuartyResult<R>) {
        _results.send(result)
        complete = true
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertResult(message: QuartyMessage.Result) = when (clazz) {
        Unit::class -> Unit as R
        else -> OBJECT_MAPPER.convertValue(message.result, clazz.java)  // For impenetrable reasons, Kotlin convertValue<> extension doesn't work properly
    }

    companion object {
        inline fun <reified R : Any> create(hostname: String): QuartyProxy<R> = QuartyProxy(
            WebsocketClientImpl.create(URI(hostname), WebsocketClientImpl.NO_RECONNECTION),
            Clock.systemUTC(),
            R::class
        )
    }
}
