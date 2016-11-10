package io.quartic.catalogue;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Environment;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;

public class CatalogueApplication extends ApplicationBase<CatalogueConfiguration> {
    private final UidGenerator<DatasetId> didGenerator = RandomUidGenerator.of(DatasetId::of);

    public static void main(String[] args) throws Exception {
        new CatalogueApplication().run(args);
    }

    public CatalogueApplication() {
        super("catalogue");
    }

    @Override
    public void run(CatalogueConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new CatalogueResource(didGenerator));
    }
}
