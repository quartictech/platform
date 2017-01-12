package io.quartic.howl;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.quartic.common.application.ApplicationBase;
import io.quartic.howl.storage.DiskStorageBackend;
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
    public void runApplication(HowlConfiguration configuration, Environment environment) {
        StorageBackend storageBackend = configuration.getStorage().build();
        environment.jersey().register(new HowlResource(storageBackend));
    }
}
