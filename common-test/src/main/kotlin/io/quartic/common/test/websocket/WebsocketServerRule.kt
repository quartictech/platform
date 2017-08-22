package io.quartic.common.test.websocket

import io.quartic.common.websocket.serverEndpointConfig
import org.glassfish.tyrus.server.TyrusServerContainer
import org.glassfish.tyrus.spi.ServerContainerFactory.createServerContainer
import org.junit.rules.ExternalResource
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.websocket.*

class WebsocketServerRule : ExternalResource() {
    private var container: TyrusServerContainer? = null
    private val _numConnections = AtomicInteger()
    private val _numDisconnections = AtomicInteger()
    private val sessions = mutableListOf<Session>()
    var messages = emptyList<String>()
    val uri by lazy { URI("ws://localhost:${container!!.port}/ws") }
    val numConnections: Int = _numConnections.get()
    val numDisconnections: Int = _numDisconnections.get()
    val receivedMessages = mutableListOf<String>()

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

    fun dropConnections() {
        sessions.forEach { it.close() }
    }

    inner class DummyEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            sessions.add(session)
            session.addMessageHandler(MessageHandler.Whole<String> { receivedMessages.add(it) })
            _numConnections.incrementAndGet()
            messages.forEach { m -> session.basicRemote.sendText(m) }
        }

        override fun onClose(session: Session, closeReason: CloseReason) {
            _numDisconnections.incrementAndGet()
        }
    }
}
