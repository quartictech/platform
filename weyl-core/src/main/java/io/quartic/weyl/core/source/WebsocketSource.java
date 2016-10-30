package io.quartic.weyl.core.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.live.LiveEvent;
import io.quartic.weyl.core.live.LiveEventConverter;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

public class WebsocketSource implements Source {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketSource.class);
    private final URI uri;
    private final LiveEventConverter converter;
    private final ObjectMapper objectMapper;
    private final Meter messageRateMeter;

    public WebsocketSource(URI uri, LiveEventConverter converter, ObjectMapper objectMapper, MetricRegistry metrics) {
        this.uri = uri;
        this.converter = converter;
        this.objectMapper = objectMapper;
        this.messageRateMeter = metrics.meter(MetricRegistry.name(WebsocketSource.class, "messages", "rate"));
    }

    @Override
    public Observable<SourceUpdate> getObservable() {
        return Observable.create(sub -> {
            final Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, message -> {
                        messageRateMeter.mark();
                        try {
                            sub.onNext(converter.toUpdate(objectMapper.readValue(message, LiveEvent.class)));
                        } catch (IOException e) {
                            e.printStackTrace();    // TODO
                        }
                    });
                }
            };

            final ClientManager clientManager = createClientManager();
            try {
                clientManager.connectToServer(endpoint, uri);
            } catch (DeploymentException | IOException e) {
                sub.onError(e);
            }
        });
    }

    private ClientManager createClientManager() {
        ClientManager clientManager = ClientManager.createClient();
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {

            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                LOG.info("Disconnecting: {}", closeReason);
                return true;
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                LOG.info("Connection failure: {}", exception);
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
