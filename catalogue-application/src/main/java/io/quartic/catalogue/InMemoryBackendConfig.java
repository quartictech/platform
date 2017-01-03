package io.quartic.catalogue;

public class InMemoryBackendConfig implements StorageBackendConfig {
    @Override
    public StorageBackend build() {
        return new InMemoryStorageBackend();
    }
}
