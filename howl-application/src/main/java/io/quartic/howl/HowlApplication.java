package io.quartic.howl;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.common.application.ApplicationBase;
import io.quartic.howl.storage.ObservableStorageBackend;
import io.quartic.howl.storage.StorageBackend;

import javax.websocket.server.ServerEndpointConfig;

import static io.quartic.common.websocket.WebsocketUtilsKt.serverEndpointConfig;

public class HowlApplication extends ApplicationBase<HowlConfiguration> {
    private WebsocketBundle websocketBundle = new WebsocketBundle(new ServerEndpointConfig[]{});

    public static void main(String[] args) throws Exception {
        new HowlApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<HowlConfiguration> bootstrap) {
        bootstrap.addBundle(websocketBundle);
    }

    @Override
    public void runApplication(HowlConfiguration configuration, Environment environment) {
        StorageBackend storageBackend = configuration.getStorage().build();
        ObservableStorageBackend observableStorageBackend = new ObservableStorageBackend(storageBackend);
        environment.jersey().register(new HowlResource(observableStorageBackend));
        websocketBundle.addEndpoint(serverEndpointConfig("/changes/{namespace}/{objectName}",
                new WebsocketEndpoint(observableStorageBackend.changes())));
    }
}
