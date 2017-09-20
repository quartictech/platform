package io.quartic.catalogue.datastore

import io.dropwizard.setup.Environment
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.StorageBackendConfig
import io.quartic.common.secrets.SecretsCodec

data class GoogleDatastoreBackendConfig(
    val projectId: String,
    val namespace: String
) : StorageBackendConfig {
    override fun build(env: Environment, secretsCodec: SecretsCodec): StorageBackend = GoogleDatastoreBackend.remote(projectId, namespace)
}
