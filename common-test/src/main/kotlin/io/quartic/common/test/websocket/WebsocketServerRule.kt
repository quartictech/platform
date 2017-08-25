package io.quartic.common.test.websocket

import io.quartic.common.logging.logger
import io.quartic.common.websocket.serverEndpointConfig
import org.glassfish.tyrus.server.TyrusServerContainer
import org.glassfish.tyrus.spi.ServerContainerFactory.createServerContainer
import org.junit.rules.ExternalResource
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.websocket.*

// TODO - reimplement with coroutines so that we can await things
class WebsocketServerRule : ExternalResource() {
    private val LOG by logger()

    private var container: TyrusServerContainer? = null
    private val _numConnections = AtomicInteger()
    private val _numDisconnections = AtomicInteger()
    private val sessions = mutableListOf<Session>()
    var messages = emptyList<String>()
    val uri by lazy { URI("ws://localhost:${container!!.port}/ws") }
    val numConnections: Int get() = _numConnections.get()
    val numDisconnections: Int get() = _numDisconnections.get()
    val receivedMessages = mutableListOf<String>()

    override fun before() {
        container = createServerContainer(null) as TyrusServerContainer
        with (container!!) {
            addEndpoint(serverEndpointConfig("/ws", DummyEndpoint()))
            start("", 0)
        }
        LOG.info("Server listening at ${uri}")
    }

    override fun after() = stop()

    fun stop() {
        container?.stop()
        container = null
    }

    @Synchronized
    fun dropConnections() {
        sessions.forEach { it.close() }
    }

    @Synchronized
    private fun addConnection(session: Session) {
        sessions.add(session)
    }

    inner class DummyEndpoint : Endpoint() {
        override fun onOpen(session: Session, config: EndpointConfig) {
            LOG.info("onOpen")

            addConnection(session)
            session.addMessageHandler(MessageHandler.Whole<String> {
                LOG.info("Received message")
                receivedMessages.add(it)
            })
            _numConnections.incrementAndGet()
            messages.forEach { m -> session.basicRemote.sendText(m) }
        }

        override fun onClose(session: Session, closeReason: CloseReason) {
            LOG.info("onClose")
            _numDisconnections.incrementAndGet()
        }
    }
}
