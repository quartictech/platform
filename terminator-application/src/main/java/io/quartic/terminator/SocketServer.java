package io.quartic.terminator;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class SocketServer {
    private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);

    private final Observable<FeatureCollectionWithTerminationId> observable;
    private final ObjectMapper objectMapper;
    private final Map<String, Subscriber<FeatureCollectionWithTerminationId>> subscribers = Maps.newConcurrentMap();

    public SocketServer(Observable<FeatureCollectionWithTerminationId> observable, ObjectMapper objectMapper) {
        this.observable = observable;
        this.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        checkArgument(!subscribers.containsKey(session.getId()), "Session already registered");

        final Subscriber<FeatureCollectionWithTerminationId> subscriber = subscriber(session);
        observable.subscribe(subscriber);
        subscribers.put(session.getId(), subscriber);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        checkArgument(subscribers.containsKey(session.getId()), "Session not registered");

        final Subscriber<FeatureCollectionWithTerminationId> subscriber = subscribers.remove(session.getId());
        subscriber.unsubscribe();
    }

    private Subscriber<FeatureCollectionWithTerminationId> subscriber(Session session) {
        return new Subscriber<FeatureCollectionWithTerminationId>() {
            @Override
            public void onCompleted() {
                LOG.error("Unexpected call to onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                LOG.error("Unexpected call to onError", e);
            }

            @Override
            public void onNext(FeatureCollectionWithTerminationId fcwdi) {
                try {
                    session.getAsyncRemote().sendText(objectMapper.writeValueAsString(fcwdi));
                } catch (JsonProcessingException e) {
                    LOG.error("Error producing JSON", e);
                }
            }
        };
    }
}
