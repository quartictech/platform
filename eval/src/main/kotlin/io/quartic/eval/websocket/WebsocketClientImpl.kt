package io.quartic.eval.websocket

import com.google.common.base.Throwables.getRootCause
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.websocket.WebsocketClient.WebsocketClientEvent
import io.quartic.eval.websocket.WebsocketClient.WebsocketClientEvent.MessageReceived
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocket
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import java.net.URI

class WebsocketClientImpl<in TSend, out TReceive>(
    uri: URI,
    private val receiveClass: Class<TReceive>
) : WebsocketClient<TSend, TReceive> {
    private sealed class WebsocketEvent {
        data class MessageReceived(val message: String) : WebsocketEvent()
        class Failed() : WebsocketEvent()
    }

    private val _outbound = Channel<TSend>(UNLIMITED)
    private val _events = Channel<WebsocketClientEvent<TReceive>>(UNLIMITED)
    private val rawEvents = Channel<WebsocketEvent>(UNLIMITED)
    override val outbound: SendChannel<TSend> = _outbound
    override val events: ReceiveChannel<WebsocketClientEvent<TReceive>> = _events
    private val LOG by logger()

    init {
        val vertx = Vertx.vertx()
        val client = vertx.createHttpClient()

        // TODO - error handling

        client.websocket(uri.port, uri.host, uri.path,
            { handleSocketOpen(it) },
            { handleSocketOpenFailure(it) }
        )
    }

    private suspend fun runEventLoop() {
        while (true) {
            select<Unit> {
                // TODO - handle messages on toServer

                // TODO - handle messages from websocket thingy

            }
        }
    }

    private fun handleSocketOpen(ws: WebSocket) {
        LOG.info("Websocket created")

        ws.handler { handleReceivedData(it) }
        ws.closeHandler { LOG.error("Websocket closed") }

        sendMessagesAsync(ws)
    }

    private fun handleSocketOpenFailure(t: Throwable) {
        LOG.error("Websocket failed to create", t)
    }

    private fun handleReceivedData(data: Buffer) {
        LOG.info("Received message")
        val parsed = try {
            OBJECT_MAPPER.readValue(data.toString(), receiveClass)
        } catch (e: Exception) {
            LOG.error("Error parsing received message", getRootCause(e))
            return
        }
        runBlocking {
            _events.send(MessageReceived(parsed))
        }
    }

    private fun sendMessagesAsync(ws: WebSocket) = async(CommonPool) {
        for (msg in _outbound) {
            LOG.info("Sending message")
            ws.writeTextMessage(OBJECT_MAPPER.writeValueAsString(msg))
        }
    }

    companion object {
        inline fun <TSend, reified TReceive> create(uri: URI): WebsocketClientImpl<TSend, TReceive> =
            WebsocketClientImpl(uri, TReceive::class.java)
    }
}
