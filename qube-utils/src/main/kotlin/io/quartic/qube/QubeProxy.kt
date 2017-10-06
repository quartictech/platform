package io.quartic.qube

import io.quartic.common.coroutines.SuspendedAutoCloseable
import io.quartic.qube.websocket.WebsocketClient
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.util.*

// Shouldn't really need this interface, but there's a current issue with coroutines and Mockito: https://github.com/mockito/mockito/issues/1152
interface QubeProxy {
    class QubeException(message: String?) : RuntimeException(message)

    sealed class QubeCompletion {
        data class Exception(val exception: QubeException): QubeCompletion()
        data class Terminated(val terminated: QubeResponse.Terminated): QubeCompletion()
    }

    class QubeContainerProxy(
        val id: UUID,
        val hostname: String,
        val errors: ReceiveChannel<QubeException>,
        private val close: suspend () -> Unit
    ) : SuspendedAutoCloseable {
        override suspend fun close() = close.invoke()
    }

    suspend fun createContainer(pod: PodSpec): QubeContainerProxy

    companion object {
        fun create(client: WebsocketClient<QubeRequest, QubeResponse>) =
            QubeProxyImpl(client)
    }
}
