package io.quartic.weyl;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feed.FeedStore;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.resource.FeedResource;
import io.quartic.weyl.resource.GeofenceResource;
import io.quartic.weyl.resource.LayerResource;
import io.quartic.weyl.resource.TileResource;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

import static io.quartic.weyl.core.utils.Utils.idSupplier;

public class WeylApplication extends Application<WeylConfiguration> {
    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
        bootstrap.addBundle(new Java8Bundle());
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
        environment.getObjectMapper().registerModule(new JavaTimeModule());

        configureCORS(environment);
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");

        environment.jersey().setUrlPattern("/api/*");

        LayerStore layerStore = new LayerStore(jdbi, idSupplier());
        LiveLayerStore liveLayerStore = new LiveLayerStore();
        environment.jersey().register(new LayerResource(layerStore, liveLayerStore));

        environment.jersey().register(new TileResource(layerStore));

        GeofenceStore geofenceStore = new GeofenceStore(liveLayerStore);
        environment.jersey().register(new GeofenceResource(geofenceStore, idSupplier()));

        FeedStore feedStore = new FeedStore(liveLayerStore, environment.getObjectMapper(), idSupplier());
        environment.jersey().register(new FeedResource(feedStore));
    }
}
