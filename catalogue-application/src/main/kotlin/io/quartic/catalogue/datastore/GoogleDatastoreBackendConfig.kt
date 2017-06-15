package io.quartic.catalogue.datastore

import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.StorageBackendConfig

data class GoogleDatastoreBackendConfig(
        val projectId: String,
        val namespace: String
) : StorageBackendConfig {
    override fun build(): StorageBackend = GoogleDatastoreBackend.remote(projectId, namespace)
}
