package io.quartic.catalogue.inmemory

import io.dropwizard.setup.Environment
import io.quartic.catalogue.StorageBackendConfig
import io.quartic.common.secrets.SecretsCodec

class InMemoryBackendConfig : StorageBackendConfig {
    override fun build(env: Environment, secretsCodec: SecretsCodec) = InMemoryStorageBackend()
}
