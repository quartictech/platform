package io.quartic.eval.qube

import io.quartic.eval.model.QubeRequest
import io.quartic.eval.model.QubeResponse
import io.quartic.eval.utils.SuspendedAutoCloseable
import io.quartic.eval.websocket.WebsocketClient
import kotlinx.coroutines.experimental.channels.ReceiveChannel

// Shouldn't really need this interface, but there's a current issue with coroutines and Mockito: https://github.com/mockito/mockito/issues/1152
interface QubeProxy {
    class QubeException(message: String) : RuntimeException(message)

    class QubeContainerProxy(
        val hostname: String,
        val errors: ReceiveChannel<QubeException>,
        private val close: suspend () -> Unit
    ) : SuspendedAutoCloseable {
        override suspend fun close() = close.invoke()
    }

    suspend fun createContainer(): QubeContainerProxy

    companion object {
        fun create(client: WebsocketClient<QubeRequest, QubeResponse>) = QubeProxyImpl(client)
    }
}
