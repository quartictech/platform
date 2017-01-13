package io.quartic.howl;

import io.dropwizard.Configuration;
import io.quartic.howl.storage.StorageBackend;

public class HowlConfiguration extends Configuration {
    StorageBackendConfig storageBackendConfig;

    public void setStorage(StorageBackendConfig storageBackendConfig) {
        this.storageBackendConfig = storageBackendConfig;
    }

    public StorageBackendConfig getStorage() {
        return storageBackendConfig;
    }
}
