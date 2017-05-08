package io.quartic.common.websocket

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.websocket.WebsocketClientSessionFactory.Companion.HEARTBEAT_INTERVAL_MILLISECONDS
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientManager.ReconnectHandler
import org.glassfish.tyrus.client.ClientProperties.RECONNECT_HANDLER
import org.glassfish.tyrus.core.TyrusSession
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import java.net.URI
import javax.websocket.ClientEndpointConfig
import javax.websocket.CloseReason
import javax.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE
import javax.websocket.Endpoint

class WebsocketClientSessionFactoryShould {

    private val session = mock<TyrusSession>()
    private val clientManager = mock<ClientManager> {
        on { connectToServer(any<Endpoint>(), any<ClientEndpointConfig>(), any<URI>()) } doReturn session
    }
    private val factory = WebsocketClientSessionFactory(javaClass, { clientManager })
    private val endpoint = mock<Endpoint>()

    @Test
    fun configure_websocket_client_correctly() {
        factory.create(endpoint, "ws://nonsense/yeah")

        verify(clientManager).connectToServer(eq(endpoint), any<ClientEndpointConfig>(), eq(URI("ws://nonsense/yeah")))
        verify(session).heartbeatInterval = HEARTBEAT_INTERVAL_MILLISECONDS
    }

    @Test
    fun set_reconnect_handler_that_always_reconnects() {
        val properties = mutableMapOf<String, Any>()
        whenever(clientManager.properties).thenReturn(properties)

        factory.create(endpoint, "ws://nonsense/yeah")

        val handler = properties[RECONNECT_HANDLER] as ReconnectHandler
        assertTrue(handler.onConnectFailure(RuntimeException()))
        assertTrue(handler.onDisconnect(CloseReason(NORMAL_CLOSURE, "foo")))
    }
}