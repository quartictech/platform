package io.quartic.weyl;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.weyl.resource.GeoJsonResource;
import io.quartic.weyl.resource.TileJsonResource;
import io.quartic.weyl.resource.TileResource;
import io.quartic.weyl.util.DataCache;
import io.quartic.weyl.util.DiskBackedDataCache;
import org.skife.jdbi.v2.DBI;

public class WeylApplication extends Application<WeylConfiguration> {
    public static void main(String[] args) throws Exception {
        new WeylApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<WeylConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void run(WeylConfiguration configuration, Environment environment) throws Exception {
        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "postgresql");

        environment.jersey().setUrlPattern("/api/*");
        DataCache cache = new DiskBackedDataCache("cache", 60 * 60 * 1000);

        TileJsonResource tileJsonResource = new TileJsonResource(configuration.getQueries());
        environment.jersey().register(tileJsonResource);

        TileResource tileResource = new TileResource(jdbi, configuration.getQueries(), cache);
        environment.jersey().register(tileResource);

        GeoJsonResource geoJsonResource = new GeoJsonResource(jdbi, configuration.getQueries(), cache);
        environment.jersey().register(geoJsonResource);
    }
}
