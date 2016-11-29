package io.quartic.howl;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.application.ApplicationBase;
import io.quartic.common.pingpong.PingPongResource;
import io.quartic.howl.storage.DiskStorageBackend;
import io.quartic.howl.storage.InMemoryStorageBackend;
import io.quartic.howl.storage.StorageBackend;

import java.nio.file.Paths;

public class HowlApplication extends ApplicationBase<HowlConfiguration> {
    public static void main(String[] args) throws Exception {
        new HowlApplication().run(args);
    }

    @Override
    public void initializeApplication(Bootstrap<HowlConfiguration> bootstrap) {
    }

    @Override
    public void runApplication(HowlConfiguration configuration, Environment environment) throws Exception {
        StorageBackend storageBackend = new DiskStorageBackend(Paths.get(configuration.getDataDir()));
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new JsonProcessingExceptionMapper(true)); // So we get Jackson deserialization errors in the response

        environment.jersey().register(new PingPongResource());
        environment.jersey().register(new HowlResource(storageBackend));
    }
}
