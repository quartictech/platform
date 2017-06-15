package io.quartic.howl

import com.fasterxml.jackson.core.JsonProcessingException
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.websocket.ResourceManagingEndpoint
import io.quartic.howl.api.StorageBackendChange
import rx.Observable
import rx.Subscription
import javax.websocket.Session

// TODO - this will need to change for 2D namespaces
class WebsocketEndpoint(private val changes: Observable<StorageBackendChange>) : ResourceManagingEndpoint<Subscription>() {
    private val LOG by logger()

    override fun createResourceFor(session: Session): Subscription {
        val namespace = session.pathParameters["namespace"]
        val objectName = session.pathParameters["objectName"]
        LOG.info("[$namespace/$objectName] changes websocket created")

        return changes
                .filter { it.namespace == namespace && it.objectName == objectName }
                .doOnEach { LOG.info("[$namespace/$objectName] change broadcast") }
                .subscribe { change ->
                    try {
                        session.asyncRemote.sendText(OBJECT_MAPPER.writeValueAsString(change))
                    } catch (e: JsonProcessingException) {
                        throw RuntimeException(e)
                    }
                }
    }

    override fun releaseResource(resource: Subscription) = resource.unsubscribe()
}
