package io.quartic.howl

import io.quartic.howl.storage.GcsStorageBackend

data class GcsStorageBackendConfig(val bucketName: String) : StorageBackendConfig {
    override fun build() = try {
        GcsStorageBackend(bucketName)
    } catch (e: Exception) {
        throw RuntimeException("Can't construct Google Cloud Storage backend", e)
    }
}
