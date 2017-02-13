package io.quartic.rain

import com.fasterxml.jackson.core.JsonFactory
import com.google.common.base.Stopwatch
import io.quartic.common.geojson.Feature
import io.quartic.common.geojson.FeatureCollection
import io.quartic.common.geojson.GeoJsonGenerator
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.websocket.ResourceManagingEndpoint
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.StorageBackendChange
import io.quartic.howl.api.StorageBackendChangeImpl
import io.quartic.weyl.api.LayerUpdateType
import io.quartic.weyl.api.LiveEvent
import io.quartic.weyl.api.LiveEventImpl
import org.slf4j.LoggerFactory
import rx.Subscription
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
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

        return listener.observable
                .startWith(StorageBackendChangeImpl.of(namespace, objectName, null))
                .doOnError { LOG.error("error: $it") }
                .subscribe({ change ->
                    LOG.info("receiving update: {}", change)
                    howlClient.downloadFile(change.namespace(), change.objectName()).use { inputStream ->
                        if (inputStream != null) {
                            val stop = Stopwatch.createStarted()
                            val parser = GeoJsonParser(inputStream)
                            try {
                                sendData(session, parser)
                            }
                            catch (e: Exception) {
                                LOG.error("exception while sending data to client", e)
                            }
                            LOG.info("took {}ms to send data", stop.elapsed(TimeUnit.MILLISECONDS))
                        }
                    }
                }, { error -> LOG.error("error: {}", error.message)})
    }

    fun sendData(session: Session, parser: GeoJsonParser) {
        val writer = session.basicRemote.sendWriter

        val liveEventGenerator = JsonFactory().createGenerator(writer)
        liveEventGenerator.codec = OBJECT_MAPPER
        with(liveEventGenerator) {
            writeStartObject()
            writeFieldName("updateType")
            writeObject(LayerUpdateType.REPLACE)
            writeFieldName("timestamp")
            writeNumber(0L)
            writeFieldName("featureCollection")
        }
        val geoJsonGenerator = GeoJsonGenerator(liveEventGenerator)
        val features = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(parser, Spliterator.ORDERED),
                false)
        geoJsonGenerator.writeFeatures(features)
        liveEventGenerator.writeEndObject()
        liveEventGenerator.flush()
        writer.flush()
        writer.close()
    }

    override fun releaseResource(resource: Subscription) {
        resource.unsubscribe()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebsocketEndpoint::class.java)
    }

}
