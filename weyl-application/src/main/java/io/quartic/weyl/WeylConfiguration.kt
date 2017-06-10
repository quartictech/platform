package io.quartic.weyl

import io.dropwizard.Configuration
import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.model.DatasetNamespace

data class WeylConfiguration(
        val catalogue: CatalogueClientConfiguration,
        val howlStorageUrl: String,
        val rainWsUrlRoot: String,
        val defaultCatalogueNamespace: DatasetNamespace,
        val exportCatalogueNamespace: DatasetNamespace,
        val map: MapConfig
) : Configuration()

data class MapConfig(
        val lat: Double,
        val lng: Double,
        val zoom: Double    // Mapbox-specific (0-20)
)
