package io.quartic.eval.websocket

import com.google.common.base.Preconditions.checkState
import com.google.common.base.Throwables.getRootCause
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.selects.select
import java.net.URI
import java.time.Duration

class WebsocketClientImpl<in TSend, out TReceive>(
    private val uri: URI,
    private val receiveClass: Class<TReceive>,
    private val backoffPeriod: Duration
) : WebsocketClient<TSend, TReceive> {

    private sealed class InternalEvent {
        data class Connected(val websocket: WebSocket) : InternalEvent()
        class FailedToConnect : InternalEvent()
        class Disconnected : InternalEvent()
        class BackoffCompleted : InternalEvent()
        data class MessageReceived(val message: String) : InternalEvent()
    }

    private sealed class SocketState {
        class Disconnected : SocketState()
        class Connecting : SocketState()
        data class Connected(val websocket: WebSocket) : SocketState()
    }

    private val manager = StateManager()
    private val httpClient = Vertx.vertx().createHttpClient()
    private val _outbound = Channel<TSend>(UNLIMITED)
    private val _events = Channel<Event<TReceive>>(UNLIMITED)
    private val internalEvents = Channel<InternalEvent>(UNLIMITED)
    override val outbound: SendChannel<TSend> = _outbound
    override val events: ReceiveChannel<Event<TReceive>> = _events
    private val LOG by logger()
    private val job = launch(CommonPool) {
        LOG.debug("Starting")
        try {
            attemptConnection()
            runEventLoop()
        } finally {
            _outbound.close()
            _events.close()
            internalEvents.close()
            httpClient.close()
        }
    }


    override fun close() {
        LOG.debug("Close")
        job.cancel()
    }

    private suspend fun runEventLoop() {
        while (true) {
            select<Unit> {
                // These events are more important, so list first in select block
                internalEvents.onReceive { manager.handleEvent(it) }
                _outbound.onReceive { maybeSendMessage(it) }
            }
        }
    }

    private suspend fun backoff() {
        async(CommonPool) {
            delay(backoffPeriod.toMillis())
            InternalEvent.BackoffCompleted().send()
        }
    }

    private suspend fun attemptConnection() {
        httpClient.websocket(uri.port, uri.host, uri.path,
            {
                LOG.info("Connected to ${uri}")
                InternalEvent.Connected(it).send()
                it.handler { InternalEvent.MessageReceived(it.toString()).send() }
                it.closeHandler { InternalEvent.Disconnected().send() }
            },
            { t ->
                LOG.error("Failed to connect", t)
                InternalEvent.FailedToConnect().send()
            }
        )
    }

    private suspend fun maybeSendMessage(message: TSend) {
        val state = manager.state
        when (state) {
            is SocketState.Connected -> {
                LOG.info("Sending message")
                try {
                    state.websocket.writeTextMessage(OBJECT_MAPPER.writeValueAsString(message)) // TODO - there is no backpressure here
                } catch (e: IllegalStateException) {}   // This occurs if the websocket is closed but we don't know it yet
            }
            else ->
                LOG.info("Swallowing message while websocket disconnected")
        }
    }

    private suspend fun Event<TReceive>.send() = _events.sendIfOpen(this)
    private fun InternalEvent.send() = runBlocking { internalEvents.sendIfOpen(this@send) }

    // To deal with stragglers after close() is called
    private suspend fun <E> SendChannel<E>.sendIfOpen(element: E) {
        try {
            send(element)
        } catch (e: ClosedSendChannelException) {}
    }

    // The only thing that can update the SocketState
    private inner class StateManager {
        private var _state: SocketState = SocketState.Connecting()
        val state get() = _state

        suspend fun handleEvent(event: InternalEvent) {
            LOG.debug("Handling event: ${event.javaClass.simpleName} (current state: ${state.javaClass.simpleName})")
            when (event) {
                is InternalEvent.Connected -> {
                    checkState(state is SocketState.Connecting, "Socket is not currently connecting")
                    _state = SocketState.Connected(event.websocket)
                    Connected<TReceive>().send()
                }
                is InternalEvent.FailedToConnect -> {
                    checkState(state is SocketState.Connecting, "Socket is not currently connecting")
                    _state = SocketState.Disconnected()
                    backoff()
                }
                is InternalEvent.Disconnected -> {
                    checkState(state is SocketState.Connected, "Socket is not currently connected")
                    _state = SocketState.Disconnected()
                    backoff()
                    Disconnected<TReceive>().send()
                }
                is InternalEvent.BackoffCompleted -> {
                    checkState(state is SocketState.Disconnected, "Socket is not currently backing off")
                    _state = SocketState.Connecting()
                    attemptConnection()
                }
                is InternalEvent.MessageReceived -> {
                    checkState(state is SocketState.Connected, "Unexpectedly received message when socket was not connected")
                    val parsed = try {
                        OBJECT_MAPPER.readValue(event.message, receiveClass)
                    } catch (e: Exception) {
                        LOG.error("Error parsing received message", getRootCause(e))
                        return
                    }
                    MessageReceived(parsed).send()
                }
            }
        }
    }

    companion object {
        inline fun <TSend, reified TReceive> create(
            uri: URI,
            backoffPeriod: Duration = Duration.ofSeconds(5)
        ): WebsocketClient<TSend, TReceive> = WebsocketClientImpl(uri, TReceive::class.java, backoffPeriod)
    }
}
