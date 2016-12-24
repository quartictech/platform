package io.quartic.common.server;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

public final class WebsocketServerUtils {
    private WebsocketServerUtils() {}

    public static ServerEndpointConfig createEndpointConfig(String path, Endpoint endpoint) {
        return ServerEndpointConfig.Builder
                .create(endpoint.getClass(), path)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) endpoint;
                    }
                })
                .build();
    }
}
