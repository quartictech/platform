package io.quartic.management;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.management.storage.GcsStorageBackend;
import io.quartic.management.storage.InMemoryStorageBackend;
import io.quartic.management.storage.StorageBackend;

public class ManagementApplication extends ApplicationBase<ManagementConfiguration> {
    public static void main(String[] args) throws Exception {
        new ManagementApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<ManagementConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }
    @Override
    public void runApplication(ManagementConfiguration configuration, Environment environment) throws Exception {
        StorageBackend storageBackend = new InMemoryStorageBackend(); //new GcsStorageBackend(configuration.getBucketName());
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(getClass(), configuration.getCatalogueUrl()));

        CatalogueService catalogueService = ClientBuilder.build(CatalogueService.class, getClass(), configuration.getCatalogueUrl());
        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new ManagementResource(catalogueService, storageBackend));
    }
}
