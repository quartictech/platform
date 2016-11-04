package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.catalogue.api.WebsocketDatasetLocator;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.live.LayerViewType;
import io.quartic.weyl.core.live.LiveEventConverter;
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
public abstract class WebsocketSource implements Source {
    public static ImmutableWebsocketSource.Builder builder() {
        return ImmutableWebsocketSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSource.class);
    protected abstract String name();
    protected abstract WebsocketDatasetLocator locator();
    protected abstract LiveEventConverter converter();
    protected abstract ObjectMapper objectMapper();
    protected abstract MetricRegistry metrics();
    @Value.Derived
    protected Meter messageRateMeter() {
        return metrics().meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate"));
    }

    @Override
    public Observable<SourceUpdate> getObservable() {
        return Observable.create(sub -> {
            final Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, message -> {
                        messageRateMeter().mark();
                        try {
                            sub.onNext(converter().toUpdate(objectMapper().readValue(message, LiveEvent.class)));
                        } catch (IOException e) {
                            e.printStackTrace();    // TODO
                        }
                    });
                }
            };

            final ClientManager clientManager = createClientManager();
            try {
                clientManager.connectToServer(endpoint, new URI(locator().url()));
            } catch (URISyntaxException | DeploymentException | IOException e) {
                sub.onError(e);
            }
        });
    }

    @Override
    public boolean indexable() {
        return false;
    }

    @Override
    public LayerViewType viewType() {
        return LayerViewType.LOCATION_AND_TRACK;
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
