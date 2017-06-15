package io.quartic.catalogue

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.quartic.catalogue.datastore.GoogleDatastoreBackendConfig
import io.quartic.catalogue.inmemory.InMemoryBackendConfig

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(
        Type(value = GoogleDatastoreBackendConfig::class, name = "google_datastore"),
        Type(value = InMemoryBackendConfig::class, name = "in_memory")
)
interface StorageBackendConfig {
    fun build(): StorageBackend
}
