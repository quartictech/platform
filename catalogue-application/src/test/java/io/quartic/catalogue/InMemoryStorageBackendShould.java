package io.quartic.catalogue;

public class InMemoryStorageBackendShould extends StorageBackendTests {
    private final InMemoryStorageBackend backend = new InMemoryStorageBackend();

    @Override
    StorageBackend getBackend() {
        return backend;
    }
}
