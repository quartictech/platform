package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Value.Immutable
public abstract class WebsocketListener {
    public static ImmutableWebsocketListener.Builder builder() {
        return ImmutableWebsocketListener.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketListener.class);

    protected abstract String name();
    protected abstract String url();
    protected abstract MetricRegistry metrics();

    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(WebsocketListener.class, "messages", "rate"));
    }

    @Value.Lazy
    public Observable<String> observable() {
        return Observable.<String>create(sub -> {
            final Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, message -> {
                        messageRateMeter().mark();
                        sub.onNext(message);
                    });
                }
            };

            final ClientManager clientManager = createClientManager();
            try {
                clientManager.connectToServer(endpoint, new URI(url()));
            } catch (URISyntaxException | DeploymentException | IOException e) {
                sub.onError(e);
            }
        }).share();
    }

    private ClientManager createClientManager() {
        ClientManager clientManager = ClientManager.createClient();
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {

            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                LOG.info("[{}] Disconnecting: {}", name(), closeReason);
                return true;
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                LOG.info("[{}] Connection failure: {}", name(), exception);
                return true;
            }

            @Override
            public long getDelay() {
                return 1;
            }
        };
        clientManager.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);
        return clientManager;
    }
}
