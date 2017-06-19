package io.quartic.howl

import io.quartic.howl.storage.DiskStorageBackend

data class DiskStorageBackendConfig(val dataDir: String = "./data") : StorageBackendConfig {
    override fun build() = DiskStorageBackend(this)
}
