package io.quartic.howl

import io.quartic.howl.storage.GcsStorageBackend

data class GcsStorageBackendConfig(val bucketSuffix: String) : StorageBackendConfig {
    override fun build() = try {
        GcsStorageBackend(this)
    } catch (e: Exception) {
        throw RuntimeException("Can't construct Google Cloud Storage backend", e)
    }
}
