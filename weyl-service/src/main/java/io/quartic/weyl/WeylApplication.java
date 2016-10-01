package io.quartic.weyl;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.resource.GeofenceResource;
import io.quartic.weyl.resource.LayerResource;
import io.quartic.weyl.resource.LiveLayerServer;
import io.quartic.weyl.resource.TileResource;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.websocket.server.ServerEndpointConfig;
import java.util.EnumSet;

public class WeylApplication extends Application<WeylConfiguration> {
    private LiveLayerStore liveLayerStore;

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(configureWebsockets());
    }

    private WebsocketBundle configureWebsockets() {
        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(LiveLayerServer.class, "/live-ws")
                .configurator(new ServerEndpointConfig.Configurator() {

                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) new LiveLayerServer(liveLayerStore);
                    }
                })
                .build();
        return new WebsocketBundle(config);
    }

    private void configureCORS(Environment environment) {
        // Enable CORS headers
        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    @Override
    public void run(WeylConfiguration configuration, Environment environment) throws Exception {
        configureCORS(environment);
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");

        environment.jersey().setUrlPattern("/api/*");

        LayerStore layerStore = new LayerStore(jdbi);
        liveLayerStore = new LiveLayerStore();
        LayerResource layerResource = new LayerResource(layerStore, liveLayerStore);
        environment.jersey().register(layerResource);

        TileResource tileResource = new TileResource(layerStore);
        environment.jersey().register(tileResource);

        GeofenceStore geofenceStore = new GeofenceStore(liveLayerStore);
        GeofenceResource geofenceResource = new GeofenceResource(geofenceStore);
        environment.jersey().register(geofenceResource);
    }
}
