package io.quartic.common.client;

import com.fasterxml.jackson.databind.JavaType;
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

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static rx.Observable.empty;
import static rx.Observable.just;

@Value.Immutable
public abstract class WebsocketListener<T> {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketListener.class);

    public static <T> WebsocketListener<T> of(String url, Class<T> type, WebsocketClientSessionFactory websocketFactory) {
        return of(url, OBJECT_MAPPER.getTypeFactory().uncheckedSimpleType(type), websocketFactory);
    }

    public static <T> WebsocketListener<T> of(String url, JavaType type, WebsocketClientSessionFactory websocketFactory) {
        return ImmutableWebsocketListener.<T>builder().type(type)
                .url(url)
                .websocketFactory(websocketFactory)
                .build();
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
            return just(OBJECT_MAPPER.readValue(message, type()));
        } catch (IOException e) {
            LOG.error("Error converting message", e);
            return empty();
        }
    }

}
