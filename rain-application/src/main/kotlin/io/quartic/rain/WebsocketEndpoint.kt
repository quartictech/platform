package io.quartic.rain

import com.fasterxml.jackson.core.JsonFactory
import com.google.common.base.Stopwatch
import io.quartic.common.geojson.GeoJsonGenerator
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.websocket.ResourceManagingEndpoint
import io.quartic.common.websocket.WebsocketClientSessionFactory
import io.quartic.common.websocket.WebsocketListener
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.StorageChange
import io.quartic.weyl.api.LayerUpdateType
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.StreamSupport
import javax.websocket.Session

class WebsocketEndpoint(private val howlWatchUrl: String,
                        private val websocketClientSessionFactory: WebsocketClientSessionFactory,
                        private val howlClient: HowlClient) : ResourceManagingEndpoint<Subscription>() {
    private val LOG by logger()

    override fun createResourceFor(session: Session): Subscription {
        val namespace = session.pathParameters["namespace"]!!
        val objectName = session.pathParameters["objectName"]!!
        val listener = WebsocketListener<StorageChange>(
                OBJECT_MAPPER.typeFactory.constructType(StorageChange::class.java),
                String.format("%s/%s/%s", howlWatchUrl, namespace, objectName),
                websocketClientSessionFactory)

        return listener.observable
                .startWith(StorageChange(namespace, objectName, null))
                .doOnError { LOG.error("error: $it") }
                .subscribe({ change ->
                    LOG.info("[{}/{}] receiving update: {}", namespace, objectName, change)
                    howlClient.downloadFile(change.namespace, change.objectName)?.use { inputStream ->
                        val stop = Stopwatch.createStarted()
                        val parser = GeoJsonParser(inputStream)
                        try {
                            sendData(session, parser)
                        } catch (e: Exception) {
                            LOG.error("[{}/{}] exception while sending data to client", namespace, objectName, e)
                        }
                        LOG.info("[{}/{}] took {}ms to send data", namespace, objectName,
                                stop.elapsed(TimeUnit.MILLISECONDS))
                    }
                }, { error -> LOG.error("[{}/{}] error: {}", namespace, objectName, error)})
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
}
