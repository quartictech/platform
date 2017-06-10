package io.quartic.weyl

import io.dropwizard.Configuration
import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.weyl.core.model.Tag

data class WeylConfiguration(
        val catalogue: CatalogueClientConfiguration,
        val howlStorageUrl: String,
        val rainWsUrlRoot: String,
        val importNamespaceRules: Map<DatasetNamespace, List<Tag>>,
        val exportNamespace: DatasetNamespace,
        val map: MapConfig
) : Configuration()

data class MapConfig(
        val lat: Double,
        val lng: Double,
        val zoom: Double    // Mapbox-specific (0-20)
)
