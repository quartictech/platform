package io.quartic.howl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quartic.howl.storage.StorageBackend;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GcsStorageBackendConfig.class, name = "google_cloud_storage"),
        @JsonSubTypes.Type(value = DiskStorageBackendConfig.class, name = "local_disk"),
})
public interface StorageBackendConfig {
    StorageBackend build();
}
