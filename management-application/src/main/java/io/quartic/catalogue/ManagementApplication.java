package io.quartic.catalogue;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;

public class ManagementApplication extends Application<ManagementConfiguration> {
    private final UidGenerator<DatasetId> didGenerator = RandomUidGenerator.of(DatasetId::of);

    public static void main(String[] args) throws Exception {
        new ManagementApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<ManagementConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(ManagementConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(configuration.getCatalogueUrl()));

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new ManagementResource(didGenerator));
    }
}
