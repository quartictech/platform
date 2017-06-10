package io.quartic.catalogue

import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates

data class CatalogueEvent(
        val type: Type,
        val coords: DatasetCoordinates,
        val config: DatasetConfig
) {
    enum class Type {
        CREATE,
        DELETE
    }
}
