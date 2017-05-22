package io.quartic.catalogue

import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetLocator.CloudGeoJsonDatasetLocator
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.catalogue.api.model.MimeType
import io.quartic.common.logging.logger
import java.io.IOException

// This is a big-bang offline conversion
class Migration {
    private val LOG by logger()

    fun migrate(backend: StorageBackend) {
        var numConverted = 0
        var numFailed = 0
        var numSkipped = 0

        LOG.info("[Migration] Running...")

        backend.all.forEach { coords, config ->
            fun String.nicely() = "[Migration][$coords] $this"
            val locator = config.locator

            if (locator is CloudGeoJsonDatasetLocator) {
                LOG.info("Processing".nicely())

                val copyCoords = coords.copy(namespace = BACKUP_NAMESPACE)

                try {
                    backend.put(copyCoords, config)

                    backend.put(coords, config.copy(locator = CloudDatasetLocator(
                            path = locator.path,
                            streaming = locator.streaming,
                            mimeType = MimeType.GEOJSON
                    )))

                    backend.remove(copyCoords)
                    numConverted++
                } catch (e: IOException) {
                    LOG.warn("Conversion failed (backup at $copyCoords)".nicely(), e)
                    numFailed++
                }
            } else {
                LOG.info("Skipping because not CloudGeoJsonDatasetLocator".nicely())
                numSkipped++
            }
        }

        LOG.info("[Migration] Finished - $numConverted converted, $numFailed failed, $numSkipped skipped")
    }

    companion object {
        val BACKUP_NAMESPACE = DatasetNamespace("__migration")
    }
}