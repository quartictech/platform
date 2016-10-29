package io.quartic.jester;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.jester.api.DatasetId;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;

public class JesterApplication extends Application<JesterConfiguration> {
    private final UidGenerator<DatasetId> didGenerator = RandomUidGenerator.of(DatasetId::of);

    public static void main(String[] args) throws Exception {
        new JesterApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JesterConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(JesterConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new JesterResource(didGenerator));
    }
}
