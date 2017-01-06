package io.quartic.terminator

import io.quartic.common.serdes.OBJECT_MAPPER
import java.util.concurrent.TimeUnit
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler.Whole
import javax.websocket.Session

class CollectingEndpoint<out T>(private val converter: (String) -> T) : Endpoint() {
    companion object {
        inline fun <reified T : Any> create(): CollectingEndpoint<T> {
            return CollectingEndpoint({ OBJECT_MAPPER.readValue(it, T::class.java) })
        }
    }

    private val _messages = mutableListOf<T>()
    val messages: List<T> get() = _messages.toList()

    override fun onOpen(session: Session, config: EndpointConfig) =
            session.addMessageHandler(Whole<String> { message -> _messages.add(converter(message)) })

    fun awaitMessages(expected: Int, timeout: Long, unit: TimeUnit): Boolean {
        var tick = timeout
        while (tick != 0L && _messages.size < expected) {
            unit.sleep(1)
            tick--
        }
        return _messages.size >= expected
    }
}
