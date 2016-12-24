package io.quartic.terminator;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.common.server.ResourceManagingEndpoint;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;

import javax.websocket.Session;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static org.slf4j.LoggerFactory.getLogger;

@Metered
@Timed
@ExceptionMetered
public class WebsocketEndpoint extends ResourceManagingEndpoint<Subscription> {
    private static final Logger LOG = getLogger(WebsocketEndpoint.class);

    private final Observable<FeatureCollectionWithTerminationId> observable;

    public WebsocketEndpoint(Observable<FeatureCollectionWithTerminationId> observable) {
        this.observable = observable;
    }

    @Override
    protected Subscription createResourceFor(Session session) {
        return observable.subscribe((fcwdi) -> {
            try {
                session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(fcwdi));
            } catch (JsonProcessingException e) {
                LOG.error("Error producing JSON", e);
            }
        });
    }

    @Override
    protected void releaseResource(Subscription subscription) {
        subscription.unsubscribe();
    }
}
