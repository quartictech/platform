package io.quartic.catalogue.migration

import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.common.logging.logger

class CloudPathMigration {
    private val LOG by logger()

    fun migrate(backend: StorageBackend) {
        LOG.info("Migration started")

        backend.getAll().forEach { coords, config ->
            val locator = config.locator
            if (locator is DatasetLocator.CloudDatasetLocator) {
                val bits = locator.path.split("/")
                if (bits.size == 3) {   // Remember to take account of the leading '/'
                    LOG.info("Updating ${locator.path}")
                    backend[coords] = config.copy(
                        locator = locator.copy(
                            path = "/${bits[1]}/${bits[1]}/${bits[2]}"
                            path = "/${bits[1]}/managed/${bits[1]}/${bits[2]}"
                        )
                    )
                } else {
                    LOG.info("Skipping ${locator.path} - already migrated")
                }
            }
        }
        LOG.info("Migration complete")
    }
}
