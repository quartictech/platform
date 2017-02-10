package io.quartic.rain

import com.fasterxml.jackson.core.JsonFactory
import io.quartic.common.geojson.GeoJsonGenerator
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.common.serdes.objectMapper
import io.quartic.common.websocket.ResourceManagingEndpoint
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.StorageBackendChange
import io.quartic.howl.api.StorageBackendChangeImpl
import org.slf4j.LoggerFactory
import rx.Subscription
import java.util.*
import java.util.stream.StreamSupport
import javax.websocket.Session

class WebsocketEndpoint(private val howlWatchUrl: String,
                        private val websocketClientSessionFactory: WebsocketClientSessionFactory,
                        private val howlClient: HowlClient) : ResourceManagingEndpoint<Subscription>() {

    override fun createResourceFor(session: Session): Subscription {
        val namespace = session.pathParameters["namespace"]
        val objectName = session.pathParameters["objectName"]
        val listener = WebsocketListener<StorageBackendChange>(
                OBJECT_MAPPER.typeFactory.uncheckedSimpleType(StorageBackendChange::class.java),
                String.format("%s/%s/%s", howlWatchUrl, namespace, objectName),
                websocketClientSessionFactory)

        return listener.observable.startWith(StorageBackendChangeImpl.of(namespace, objectName, null))
                .subscribe { change ->
                    LOG.info("receiving update: {}", change)
                    val inputStream = howlClient.downloadFile(change.namespace(), change.objectName())
                    if (inputStream != null) {
                        val parser = GeoJsonParser(inputStream)

                        session.basicRemote.sendStream.use { outputStream ->
                            val liveEventGenerator = JsonFactory().createGenerator(outputStream)
                            liveEventGenerator.writeStartObject()
                            liveEventGenerator.writeFieldName("timestamp")
                            liveEventGenerator.writeNumber(0L)
                            liveEventGenerator.writeFieldName("featureCollection")
                            liveEventGenerator.flush()
                            val geoJsonGenerator = GeoJsonGenerator(outputStream)
                            val features = StreamSupport.stream(
                                    Spliterators.spliteratorUnknownSize(parser, Spliterator.ORDERED),
                                    false)
                            geoJsonGenerator.writeFeatures(features)
                            liveEventGenerator.writeEndObject()
                            liveEventGenerator.flush()
                        }
                    }
                }
    }

    override fun releaseResource(resource: Subscription) {
        resource.unsubscribe()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebsocketEndpoint::class.java)
        private val OBJECT_MAPPER = objectMapper()
    }

}
