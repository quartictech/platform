package io.quartic.eval.websocket

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

interface WebsocketClient<in TSend, out TReceive> {
    sealed class WebsocketClientEvent<out T> {
        class BecomeReady<T> : WebsocketClientEvent<T>()
        class BecomeFailed<T> : WebsocketClientEvent<T>()
        data class MessageReceived<T>(val message: T) : WebsocketClientEvent<T>()
    }

    val outbound: SendChannel<TSend>
    val events: ReceiveChannel<WebsocketClientEvent<TReceive>>

}
