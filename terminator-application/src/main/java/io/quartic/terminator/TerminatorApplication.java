package io.quartic.terminator;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.pingpong.PingPongResource;

public class TerminatorApplication extends Application<TerminatorConfiguration> {
    public static void main(String[] args) throws Exception {
        new TerminatorApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<TerminatorConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(TerminatorConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new TerminatorResource());
    }
}
