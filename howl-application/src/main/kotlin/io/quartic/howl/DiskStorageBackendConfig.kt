package io.quartic.howl

import io.quartic.howl.storage.DiskStorageBackend
import java.nio.file.Paths

data class DiskStorageBackendConfig(val dataDir: String = "./data") : StorageBackendConfig {
    override fun build() = DiskStorageBackend(Paths.get(dataDir))
}
