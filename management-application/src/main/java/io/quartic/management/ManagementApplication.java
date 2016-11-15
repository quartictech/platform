package io.quartic.management;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Environment;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.ClientBuilder;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.common.pingpong.PingPongResource;

public class ManagementApplication extends ApplicationBase<ManagementConfiguration> {
    public static void main(String[] args) throws Exception {
        new ManagementApplication().run(args);
    }

    @Override
    public void runApplication(ManagementConfiguration configuration, Environment environment) throws Exception {
        GcsConnector gcsConnector = new GcsConnector(configuration.getBucketName());
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(getClass(), configuration.getCatalogueUrl()));

        CatalogueService catalogueService = ClientBuilder.build(CatalogueService.class, getClass(), configuration.getCatalogueUrl());
        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new ManagementResource(catalogueService, gcsConnector));
    }
}
