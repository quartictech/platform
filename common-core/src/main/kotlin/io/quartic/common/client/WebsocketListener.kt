package io.quartic.common.client

import com.fasterxml.jackson.databind.JavaType
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.serdes.objectMapper
import rx.Emitter.BackpressureMode
import rx.Observable
import rx.Observable.*
import java.io.IOException
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler.Whole
import javax.websocket.Session

class WebsocketListener<T>(
        private val type: JavaType,
        private val url: String,
        private val websocketFactory: WebsocketClientSessionFactory
) {
    private val LOG by logger()

    class Factory(private val url: String, private val websocketFactory: WebsocketClientSessionFactory) {
        fun <T> create(type: Class<T>): WebsocketListener<T> {
            return create(OBJECT_MAPPER.typeFactory.uncheckedSimpleType(type))
        }

        fun <T> create(type: JavaType): WebsocketListener<T> {
            return WebsocketListener(type = type, url = url, websocketFactory = websocketFactory)
        }
    }

    val observable: Observable<T> by lazy {
        emitter.flatMap({ this.convert(it) }).share()
    }

    private val emitter: Observable<String>
        get() {
            return fromEmitter({ sub ->
                val endpoint = object : Endpoint() {
                    override fun onOpen(session: Session, config: EndpointConfig) {
                        session.addMessageHandler(Whole<String> { sub.onNext(it) })
                    }
                }

                try {
                    websocketFactory.create(endpoint, url)
                } catch (e: Exception) {
                    sub.onError(e)
                }
            }, BackpressureMode.BUFFER)
        }


    private fun convert(message: String) = try {
        just(objectMapper().readValue<T>(message, type))
    } catch (e: IOException) {
        LOG.error("Error converting message", e)
        empty<T>()
    }
}
