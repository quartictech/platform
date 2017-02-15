package io.quartic.common.test.websocket

import io.quartic.common.websocket.serverEndpointConfig
import org.glassfish.tyrus.server.TyrusServerContainer
import org.glassfish.tyrus.spi.ServerContainerFactory.createServerContainer
import org.junit.rules.ExternalResource
import java.util.concurrent.atomic.AtomicInteger
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Session

class WebsocketServerRule : ExternalResource() {
    private var container: TyrusServerContainer? = null
    private val _numConnections = AtomicInteger()
    private val _numDisconnections = AtomicInteger()
    var messages = emptyList<String>()
    val uri: String get() = "ws://localhost:${container!!.port}/ws"
    val numConnections: Int get() = _numConnections.get()
    val numDisconnections: Int get() = _numDisconnections.get()

    override fun before() {
        container = createServerContainer(null) as TyrusServerContainer
        with (container!!) {
            addEndpoint(serverEndpointConfig("/ws", DummyEndpoint()))
            start("", 0)
        }
    }

    override fun after() {
        container?.stop()
        container = null
    }

    inner class DummyEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            _numConnections.incrementAndGet()
            messages.forEach { m -> session.basicRemote.sendText(m) }
        }

        override fun onClose(session: Session, closeReason: CloseReason) {
            _numDisconnections.incrementAndGet()
        }
    }
}
