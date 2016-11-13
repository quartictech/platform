package io.quartic.common.client;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.net.HttpHeaders.USER_AGENT;

public class WebsocketClientSessionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketClientSessionFactory.class);

    public final Class<?> owner;

    public WebsocketClientSessionFactory(Class<?> owner) {
        this.owner = owner;
    }

    public Session create(Endpoint endpoint, String url) throws URISyntaxException, IOException, DeploymentException {
        return clientManager()
                .connectToServer(endpoint, websocketClientConfig(), new URI(url));
    }

    private ClientEndpointConfig websocketClientConfig() {
        return ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put(USER_AGENT, newArrayList(Utils.userAgentFor(owner)));
                    }
                })
                .build();
    }

    private ClientManager clientManager() {
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
