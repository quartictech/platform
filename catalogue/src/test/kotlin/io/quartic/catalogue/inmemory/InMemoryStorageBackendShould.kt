package io.quartic.catalogue.inmemory

import io.quartic.catalogue.StorageBackendTests

class InMemoryStorageBackendShould : StorageBackendTests() {
    override val backend by lazy { InMemoryStorageBackend() }
}
