package io.quartic.weyl

import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.application.ConfigurationBase
import io.quartic.weyl.core.model.Tag
import java.net.URI

data class WeylConfiguration(
    val catalogue: CatalogueClientConfiguration,
    val howlStorageUrl: URI,
    val rainWsUrlRoot: String,
    val importNamespaceRules: Map<DatasetNamespace, List<Tag>>,
    val exportNamespace: DatasetNamespace,
    val map: MapConfig
) : ConfigurationBase()

data class MapConfig(
        val lat: Double,
        val lng: Double,
        val zoom: Double    // Mapbox-specific (0-20)
)
