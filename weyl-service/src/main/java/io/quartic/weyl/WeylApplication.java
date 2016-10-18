package io.quartic.weyl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.websockets.WebsocketBundle;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.live.LiveEventId;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.core.utils.RandomUidGenerator;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;
import io.quartic.weyl.resource.*;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.websocket.server.ServerEndpointConfig;
import java.util.EnumSet;
import java.util.function.Supplier;

public class WeylApplication extends Application<WeylConfiguration> {
    private UpdateServer updateServer = null;   // TODO: deal with weird mutability

    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(configureWebsockets(bootstrap.getObjectMapper()));
    }

    private WebsocketBundle configureWebsockets(ObjectMapper objectMapper) {
        updateServer = new UpdateServer(objectMapper);
        final ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(UpdateServer.class, "/ws")
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                        return (T) updateServer;
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

        environment.jersey().setUrlPattern("/api/*");

        final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
        final UidGenerator<LayerId> lidGenerator = RandomUidGenerator.of(LayerId::of);   // Use a random generator to ensure MapBox tile caching doesn't break things
        final UidGenerator<LiveEventId> eidGenerator = SequenceUidGenerator.of(LiveEventId::of);

        final FeatureStore featureStore = new FeatureStore(fidGenerator);
        final LayerStore layerStore = new LayerStore(featureStore, lidGenerator);
        final GeofenceStore geofenceStore = new GeofenceStore(layerStore);
        final AlertProcessor alertProcessor = new AlertProcessor(geofenceStore);

        // TODO: deal with weird mutability
        updateServer.setLayerStore(layerStore);
        alertProcessor.addListener(updateServer);

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new LayerResource(layerStore, fidGenerator, eidGenerator));
        environment.jersey().register(new TileResource(layerStore));
        environment.jersey().register(new GeofenceResource(geofenceStore));
        environment.jersey().register(new AlertResource(alertProcessor));
        environment.jersey().register(new AggregatesResource(featureStore));
        environment.jersey().register(new AttributesResource(featureStore));
        environment.jersey().register(new ImportResource(layerStore, createDbiSupplier(configuration, environment),
                featureStore, environment.getObjectMapper()));

        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response
    }

    // We pass a memoized supplier so we get connect-on-demand, to avoid startup failure when Postgres is down
    private Supplier<DBI> createDbiSupplier(WeylConfiguration configuration, Environment environment) {
        final DBIFactory factory = new DBIFactory();
        final Supplier<DBI> unmemoized = () -> factory.build(environment, configuration.getDataSourceFactory(), "postgresql");
        return Suppliers.memoize(unmemoized::get)::get;
    }
}
