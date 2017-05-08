package io.quartic.catalogue.inmemory;

import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.StorageBackendTests;

public class InMemoryStorageBackendShould extends StorageBackendTests {
    private final InMemoryStorageBackend backend = new InMemoryStorageBackend();

    @Override
    protected StorageBackend getBackend() {
        return backend;
    }
}
