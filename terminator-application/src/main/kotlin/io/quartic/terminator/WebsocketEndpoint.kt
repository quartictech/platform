package io.quartic.terminator

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Metered
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.core.JsonProcessingException
import io.quartic.common.logging.logger
import io.quartic.common.serdes.objectMapper
import io.quartic.common.server.ResourceManagingEndpoint
import io.quartic.terminator.api.FeatureCollectionWithTerminationId
import rx.Observable
import rx.Subscription
import javax.websocket.Session

@Metered
@Timed
@ExceptionMetered
class WebsocketEndpoint(private val observable: Observable<FeatureCollectionWithTerminationId>) : ResourceManagingEndpoint<Subscription>() {
    private val LOG by logger()

    override fun createResourceFor(session: Session) = observable.subscribe { fcwdi ->
        try {
            session.asyncRemote.sendText(objectMapper().writeValueAsString(fcwdi))
        } catch (e: JsonProcessingException) {
            LOG.error("Error producing JSON", e)
        }
    }

    override fun releaseResource(subscription: Subscription) = subscription.unsubscribe()
}
