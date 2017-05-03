package io.quartic.weyl.core.export

import io.quartic.catalogue.api.model.DatasetLocator

data class LayerExportResult(
        val locator: DatasetLocator,
        val message: String
)
