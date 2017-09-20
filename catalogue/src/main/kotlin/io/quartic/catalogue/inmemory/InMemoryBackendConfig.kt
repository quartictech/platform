package io.quartic.catalogue.inmemory

import io.quartic.catalogue.StorageBackendConfig

class InMemoryBackendConfig : StorageBackendConfig {
    override fun build() = InMemoryStorageBackend()
}
