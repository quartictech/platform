package io.quartic.catalogue.inmemory;

import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.StorageBackendConfig;

public class InMemoryBackendConfig implements StorageBackendConfig {
    @Override
    public StorageBackend build() {
        return new InMemoryStorageBackend();
    }
}
