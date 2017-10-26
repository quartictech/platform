package io.quartic.qube.websocket

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

interface WebsocketClient<in TSend, out TReceive> : AutoCloseable {
    sealed class Event<out T> {
        class Connected<out T> : Event<T>()
        class Disconnected<out T> : Event<T>()
        class Aborted<out T> : Event<T>()
        data class MessageReceived<out T>(val message: T) : Event<T>()
    }

    val outbound: SendChannel<TSend>
    val events: ReceiveChannel<Event<TReceive>>

}
