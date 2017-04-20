package io.quartic.weyl

import io.dropwizard.Configuration
import io.quartic.catalogue.CatalogueClientConfiguration
import io.quartic.catalogue.api.DatasetNamespace

import javax.validation.Valid
import javax.validation.constraints.NotNull

class WeylConfiguration : Configuration() {
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
}
