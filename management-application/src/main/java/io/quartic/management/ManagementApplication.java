package io.quartic.management;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.client.ClientUtilsKt;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.howl.api.HowlClient;
import io.quartic.howl.api.HowlService;

import static io.quartic.common.client.ClientUtilsKt.userAgentFor;

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
        HowlService howlService = new HowlClient(userAgentFor(ManagementApplication.class), configuration.getHowlUrl());

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(getClass(), configuration.getCatalogueUrl()));

        CatalogueService catalogueService = ClientUtilsKt.client(CatalogueService.class, getClass(), configuration.getCatalogueUrl());
        environment.jersey().register(new ManagementResource(catalogueService, howlService));
    }
}
