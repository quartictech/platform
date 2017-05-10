package io.quartic.weyl

import io.dropwizard.Configuration
import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.model.DatasetNamespace

import javax.validation.Valid
import javax.validation.constraints.NotNull

class WeylConfiguration : Configuration() {
    data class MapConfig(
            val lat: Double,
            val lng: Double,
            val zoom: Double    // Mapbox-specific (0-20)
    )

    @Valid
    @NotNull
    var catalogue: CatalogueClientConfiguration? = null

    @Valid
    @NotNull
    var howlStorageUrl: String? = null

    @Valid
    @NotNull
    var rainWsUrlRoot: String? = null

    @Valid
    @NotNull
    var defaultCatalogueNamespace: DatasetNamespace? = null

    @Valid
    @NotNull
    var exportCatalogueNamespace: DatasetNamespace? = null

    @Valid
    @NotNull
    var map: MapConfig? = null
}
