package io.quartic.common.websocket

import io.quartic.common.logging.logger
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientManager.ReconnectHandler
import org.glassfish.tyrus.client.ClientProperties.RECONNECT_HANDLER
import org.glassfish.tyrus.core.TyrusSession
import java.net.URI
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.Session

class WebsocketClientSessionFactory @JvmOverloads constructor(
        private val owner: Class<*>,
        private val clientManagerSupplier: () -> ClientManager = { ClientManager.createClient() }
) {
    private val LOG by logger()

    fun create(endpoint: Endpoint, url: String): Session {
        val session = clientManager(url).connectToServer(endpoint, clientEndpointConfig(owner), URI(url)) as TyrusSession
        session.heartbeatInterval = HEARTBEAT_INTERVAL_MILLISECONDS
        return session
    }

    // TODO: can we have a global ClientManager somehow?
    private fun clientManager(url: String): ClientManager {
        val clientManager = clientManagerSupplier()
        clientManager.properties[RECONNECT_HANDLER] = reconnectHandler(url)
        return clientManager
    }

    private fun reconnectHandler(url: String) = object : ReconnectHandler() {
        override fun onDisconnect(closeReason: CloseReason): Boolean {
            LOG.warn("[$url] Disconnecting: $closeReason")
            return true
        }

        override fun onConnectFailure(exception: Exception): Boolean {
            LOG.warn("[$url] Connection failure", exception)
            return true
        }

        override fun getDelay(): Long = 5
    }

    companion object {
        val HEARTBEAT_INTERVAL_MILLISECONDS = 30000L
    }
}
