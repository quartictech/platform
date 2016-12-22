package io.quartic.common.client;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
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
        return clientManager(url)
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

    private ClientManager clientManager(String url) {
        ClientManager clientManager = ClientManager.createClient();
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {

            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                LOG.warn("[{}] Disconnecting: {}", url, closeReason);
                return true;
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                LOG.warn("[{}] Connection failure: {}\n{}",
                        url,
                        exception.getMessage(),
                        formatStackTrace(exception.getStackTrace()));
                return true;
            }

            @Override
            public long getDelay() {
                return 5;
            }
        };
        clientManager.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);
        return clientManager;
    }

    private String formatStackTrace(StackTraceElement[] stackTrace) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
            sb.append("\tat " + stackTrace[i] + "\n");
        }
        return sb.append("\t...").toString();
    }
}
