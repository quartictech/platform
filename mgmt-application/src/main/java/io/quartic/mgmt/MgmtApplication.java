package io.quartic.mgmt;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.healthcheck.PingPongHealthCheck;
import io.quartic.howl.api.HowlClient;
import io.quartic.howl.api.HowlService;

import static io.quartic.common.client.ClientUtilsKt.client;
import static io.quartic.common.client.ClientUtilsKt.userAgentFor;

public class MgmtApplication extends ApplicationBase<MgmtConfiguration> {
    public static void main(String[] args) throws Exception {
        new MgmtApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<MgmtConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void runApplication(MgmtConfiguration configuration, Environment environment) {
        HowlService howlService = new HowlClient(userAgentFor(MgmtApplication.class), configuration.getHowlUrl());

        environment.healthChecks().register("catalogue", new PingPongHealthCheck(getClass(), configuration.getCatalogueUrl()));

        CatalogueService catalogueService = client(CatalogueService.class, getClass(), configuration.getCatalogueUrl());
        environment.jersey().register(new MgmtResource(catalogueService, howlService, configuration.getDefaultCatalogueNamespace()));
    }
}
