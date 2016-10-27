package io.quartic.jester;

import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.pingpong.PingPongResource;

public class JesterApplication extends Application<JesterConfiguration> {
    public static void main(String[] args) throws Exception {
        new JesterApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JesterConfiguration> bootstrap) {
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(JesterConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new PingPongResource());
    }
}
