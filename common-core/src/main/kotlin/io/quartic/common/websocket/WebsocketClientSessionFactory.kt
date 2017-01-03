package io.quartic.common.websocket

import io.quartic.common.logging.logger
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientManager.ReconnectHandler
import org.glassfish.tyrus.client.ClientProperties.RECONNECT_HANDLER
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.websocket.CloseReason
import javax.websocket.DeploymentException
import javax.websocket.Endpoint

class WebsocketClientSessionFactory(val owner: Class<*>) {
    private val LOG by logger()

    // TODO: remove exception signature once everything converted to Kotlin
    @Throws(URISyntaxException::class, IOException::class, DeploymentException::class)
    fun create(endpoint: Endpoint, url: String) =
            clientManager(url).connectToServer(endpoint, clientEndpointConfig(owner), URI(url))

    private fun clientManager(url: String): ClientManager {
        val clientManager = ClientManager.createClient()
        clientManager.properties.put(RECONNECT_HANDLER, reconnectHandler(url))
        return clientManager
    }

    private fun reconnectHandler(url: String) = object : ReconnectHandler() {

        override fun onDisconnect(closeReason: CloseReason): Boolean {
            LOG.warn("[{}] Disconnecting: {}", url, closeReason)
            return true
        }

        override fun onConnectFailure(exception: Exception): Boolean {
            LOG.warn("[{}] Connection failure: {}\n{}", url, exception.message, formatStackTrace(exception.stackTrace))
            return true
        }

        override fun getDelay(): Long = 5
    }

    private fun formatStackTrace(stackTrace: Array<StackTraceElement>): String {
        val sb = StringBuilder()
        for (i in 0..Math.min(3, stackTrace.size) - 1) {
            sb.append("\tat " + stackTrace[i] + "\n")
        }
        return sb.append("\t...").toString()
    }
}
