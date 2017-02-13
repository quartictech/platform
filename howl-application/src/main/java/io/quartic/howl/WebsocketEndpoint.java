package io.quartic.howl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.common.websocket.ResourceManagingEndpoint;
import io.quartic.howl.api.StorageBackendChange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import javax.websocket.Session;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;

public class WebsocketEndpoint extends ResourceManagingEndpoint<Subscription> {
    private final static Logger LOG = LoggerFactory.getLogger(WebsocketEndpoint.class);
    private static final ObjectMapper OBJECT_MAPPER = objectMapper();
    private final Observable<StorageBackendChange> changes;

    public WebsocketEndpoint(Observable<StorageBackendChange> changes) {
        this.changes = changes;
    }

    @Override
    protected Subscription createResourceFor(@NotNull Session session) {
        String namespace = session.getPathParameters().get("namespace");
        String objectName = session.getPathParameters().get("objectName");
        LOG.info("[{}/{}] changes websocket created", namespace, objectName);

        Subscription subscription = changes
                .doOnEach(change -> LOG.info("[{}/{}] change broadcast", namespace, objectName))
                .filter(change -> change.namespace().equals(namespace) &&
                        change.objectName().equals(objectName))
                .subscribe(change -> {
                    try {
                        session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(change));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        return subscription;
    }

    @Override
    protected void releaseResource(Subscription resource) {
        resource.unsubscribe();
    }

}
