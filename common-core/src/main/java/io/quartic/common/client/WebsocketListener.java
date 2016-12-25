package io.quartic.common.client;

import com.fasterxml.jackson.databind.JavaType;
import io.quartic.common.serdes.ObjectMappersKt;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URISyntaxException;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static rx.Observable.empty;
import static rx.Observable.just;

@Value.Immutable
public abstract class WebsocketListener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketListener.class);

    @Value.Immutable
    public static abstract class Factory {
        public static Factory of(String url, WebsocketClientSessionFactory websocketFactory) {
            return ImmutableFactory.of(url, websocketFactory);
        }

        @Value.Parameter
        protected abstract String url();

        @Value.Parameter
        protected abstract WebsocketClientSessionFactory websocketFactory();

        public <T> WebsocketListener<T> create(Class<T> type) {
            return create(objectMapper().getTypeFactory().uncheckedSimpleType(type));
        }

        public <T> WebsocketListener<T> create(JavaType type) {
            return ImmutableWebsocketListener.<T>builder().type(type)
                    .url(url())
                    .websocketFactory(websocketFactory())
                    .build();
        }
    }

    protected abstract JavaType type();
    protected abstract String url();
    protected abstract WebsocketClientSessionFactory websocketFactory();

    @Value.Lazy
    public Observable<T> observable() {
        return Observable.<String>create(sub -> {
            final Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, sub::onNext);
                }
            };

            try {
                websocketFactory().create(endpoint, url());
            } catch (URISyntaxException | DeploymentException | IOException e) {
                sub.onError(e);
            }
        }).flatMap(this::convert).share();
    }


    private Observable<T> convert(String message) {
        try {
            return just(objectMapper().readValue(message, type()));
        } catch (IOException e) {
            LOG.error("Error converting message", e);
            return empty();
        }
    }

}
