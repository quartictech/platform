package io.quartic.eval.websocket

import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import java.net.URI

/**
 * This class exists so that we can mock out real websocket stuff while testing [WebsocketClientImpl], as a real
 * websocket is super flaky, and [io.vertx.core.http.WebSocket] can't be mocked.
 */
class WebsocketFactory : AutoCloseable {
    class Websocket(private val ws: WebSocket) {
        fun writeTextMessage(text: String) {
            ws.writeTextMessage(text)
        }
    }

    private val httpClient = Vertx.vertx().createHttpClient()

    override fun close() {
        httpClient.close()
    }

    fun create(
        uri: URI,
        connectHandler: (Websocket) -> Unit,
        failureHandler: (Throwable) -> Unit,
        messageHandler: (String) -> Unit,
        closeHandler: () -> Unit
    ) {
        httpClient.websocket(uri.port, uri.host, uri.path,
            {
                connectHandler(Websocket(it))
                it.handler { messageHandler(it.toString()) }
                it.closeHandler { closeHandler() }
            },
            { failureHandler(it) }
        )
    }
}
