package io.quartic.common.server;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.function.Supplier;

public final class WebsocketServerUtils {
    private WebsocketServerUtils() {}

    public static ServerEndpointConfig createEndpointConfig(String path, Endpoint endpoint) {
        return createEndpointConfig(path, () -> endpoint);
    }

    public static ServerEndpointConfig createEndpointConfig(String path, Supplier<Endpoint> endpoint) {
        return ServerEndpointConfig.Builder
                .create(endpoint.get().getClass(), path)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) endpoint.get();
                    }
                })
                .build();
    }
}
