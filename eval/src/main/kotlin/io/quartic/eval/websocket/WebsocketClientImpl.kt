package io.quartic.eval.websocket

import com.google.common.base.Preconditions.checkState
import com.google.common.base.Throwables.getRootCause
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.websocket.WebsocketClient.Event
import io.quartic.eval.websocket.WebsocketClient.Event.*
import io.quartic.eval.websocket.WebsocketFactory.Websocket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.selects.select
import java.net.URI
import java.time.Duration

// TODO - set user-agent

class WebsocketClientImpl<in TSend, out TReceive>(
    private val uri: URI,
    private val receiveClass: Class<TReceive>,
    private val backoffPeriod: Duration,
    private val websocketFactory: WebsocketFactory
) : WebsocketClient<TSend, TReceive> {

    private sealed class InternalEvent {
        data class Connected(val websocket: Websocket) : InternalEvent()
        class FailedToConnect : InternalEvent()
        class Disconnected : InternalEvent()
        class BackoffCompleted : InternalEvent()
        data class MessageReceived(val message: String) : InternalEvent()
    }

    private sealed class SocketState {
        class Disconnected : SocketState()
        class Connecting : SocketState()
        class Aborted : SocketState()
        data class Connected(val websocket: Websocket) : SocketState()
    }

    private val manager = StateManager()
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
            websocketFactory.close()
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

    private suspend fun backoffAsync() = async(CommonPool) {
        delay(backoffPeriod.toMillis())
        send(InternalEvent.BackoffCompleted())
    }

    private suspend fun attemptConnection() {
        websocketFactory.create(uri,
            connectHandler = {
                LOG.info("Connected to ${uri}")
                send(InternalEvent.Connected(it))
            },
            failureHandler = {
                LOG.error("Failed to connect to ${uri}")
                send(InternalEvent.FailedToConnect())
            },
            messageHandler = { send(InternalEvent.MessageReceived(it)) },
            closeHandler = { send(InternalEvent.Disconnected()) }
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

    private suspend fun send(event: Event<TReceive>) = _events.sendIfOpen(event)
    private fun send(event: InternalEvent) = runBlocking { internalEvents.sendIfOpen(event) }

    // To deal with stragglers after close() is called
    private suspend fun <E> SendChannel<E>.sendIfOpen(element: E) {
        try { send(element) }
        catch (e: ClosedSendChannelException) {}
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
                    send(Connected())
                }
                is InternalEvent.FailedToConnect -> {
                    checkState(state is SocketState.Connecting, "Socket is not currently connecting")
                    abortOrBackoff()
                }
                is InternalEvent.Disconnected -> {
                    checkState(state is SocketState.Connected, "Socket is not currently connected")
                    send(Disconnected())
                    abortOrBackoff()
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
                    send(MessageReceived(parsed))
                }
            }
        }

        private suspend fun WebsocketClientImpl<TSend, TReceive>.abortOrBackoff() {
            if (backoffPeriod == ABORT_ON_FAILURE) {
                _state = SocketState.Aborted()
                send(Aborted())
            } else {
                _state = SocketState.Disconnected()
                backoffAsync()
            }
        }
    }

    companion object {
        inline fun <TSend, reified TReceive> create(
            uri: URI,
            backoffPeriod: Duration = Duration.ofSeconds(5),
            websocketFactory: WebsocketFactory = WebsocketFactory()
        ): WebsocketClient<TSend, TReceive> = WebsocketClientImpl(uri, TReceive::class.java, backoffPeriod, websocketFactory)

        val ABORT_ON_FAILURE: Duration = Duration.ZERO
    }
}
