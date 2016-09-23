package io.quartic.weyl;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.resource.LayerResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.util.DataCache;
import io.quartic.weyl.util.DiskBackedDataCache;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

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
        configureCORS(environment);
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");

        environment.jersey().setUrlPattern("/api/*");
        DataCache cache = new DiskBackedDataCache("cache", 60 * 60 * 1000);

        LayerStore layerStore = new LayerStore(jdbi);
        LiveLayerStore liveLayerStore = new LiveLayerStore();
        LayerResource layerResource = new LayerResource(layerStore, liveLayerStore);
        environment.jersey().register(layerResource);

        TileResource tileResource = new TileResource(layerStore);
        environment.jersey().register(tileResource);

    }
}
