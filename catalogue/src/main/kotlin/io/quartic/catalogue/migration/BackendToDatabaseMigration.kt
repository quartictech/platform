package io.quartic.catalogue.migration

import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.database.Database
import io.quartic.common.logging.logger

class BackendToDatabaseMigration {
    private val LOG by logger()

    fun migrate(backend: StorageBackend, database: Database) {
        LOG.info("Migration started")

        backend.getAll().forEach { coords, config ->
            if (database.getDataset(coords.namespace.namespace, coords.id.uid) != null) {
                LOG.info("Skipping ${coords}")
            } else {
                LOG.info("Migrating ${coords}")
                database.insertDataset(
                    namespace = coords.namespace.namespace,
                    id = coords.id.uid,
                    config = config
                )
            }
        }

        LOG.info("Migration complete")
    }
}
