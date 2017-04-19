package io.quartic.catalogue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.catalogue.datastore.GoogleDatastoreBackendConfig;
import io.quartic.catalogue.inmemory.InMemoryBackendConfig;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GoogleDatastoreBackendConfig.class, name = "google_datastore"),
        @JsonSubTypes.Type(value = InMemoryBackendConfig.class, name = "in_memory"),
})
public interface StorageBackendConfig {
    StorageBackend build();
}
