package io.quartic.catalogue.io.quartic.catalogue.datastore;

import io.quartic.catalogue.StorageBackend;
import io.quartic.catalogue.StorageBackendConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class GoogleDatastoreBackendConfig implements StorageBackendConfig {
    @Valid
    @NotNull
    private String projectId;

    @Valid
    @NotNull
    private String namespace;

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public StorageBackend build() {
        return GoogleDatastoreBackend.remote(projectId, namespace);
    }
}
