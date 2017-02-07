package io.quartic.catalogue

import io.quartic.catalogue.api.DatasetConfig
import io.quartic.catalogue.api.DatasetId

data class CatalogueEvent(
        val type: Type,
        val id: DatasetId,
        val config: DatasetConfig
) {
    enum class Type {
        CREATE,
        DELETE
    }
}
