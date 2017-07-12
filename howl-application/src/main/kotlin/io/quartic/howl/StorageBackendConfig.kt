package io.quartic.howl

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.howl.storage.StorageBackend

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
        Type(value = GcsStorageBackendConfig::class, name = "google_cloud_storage"),
        Type(value = DiskStorageBackendConfig::class, name = "local_disk")
)
interface StorageBackendConfig {
    fun build(): StorageBackend
}
