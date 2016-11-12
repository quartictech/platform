package io.quartic.common.client;

import org.immutables.value.Value;
import rx.Observable;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URISyntaxException;

@Value.Immutable
public abstract class WebsocketListener {
    public static ImmutableWebsocketListener.Builder builder() {
        return ImmutableWebsocketListener.builder();
    }

    protected abstract String name();
    protected abstract String url();
    protected abstract WebsocketClientSessionFactory websocketFactory();

    @Value.Lazy
    public Observable<String> observable() {
        return Observable.<String>create(sub -> {
            final Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, sub::onNext);
                }
            };

            try {
                websocketFactory().create(endpoint, url(), name());
            } catch (URISyntaxException | DeploymentException | IOException e) {
                sub.onError(e);
            }
        }).share();
    }
}
