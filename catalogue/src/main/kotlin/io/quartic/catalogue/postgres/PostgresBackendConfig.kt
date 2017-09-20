package io.quartic.catalogue.inmemory

import io.dropwizard.setup.Environment
import io.quartic.catalogue.CatalogueApplication
import io.quartic.catalogue.StorageBackendConfig
import io.quartic.catalogue.postgres.Database
import io.quartic.catalogue.postgres.PostgresBackend
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.DatabaseConfiguration
import io.quartic.common.secrets.SecretsCodec

class PostgresBackendConfig(
    val database: DatabaseConfiguration
) : StorageBackendConfig {
    override fun build(env: Environment, secretsCodec: SecretsCodec): PostgresBackend {
        val db = DatabaseBuilder(
            CatalogueApplication::class.java,
            database,
            env,
            secretsCodec
        ).dao<Database>()

        return PostgresBackend(db)
    }
}
