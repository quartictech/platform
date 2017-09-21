package io.quartic.home.model

import io.quartic.catalogue.api.model.DatasetMetadata

data class CreateDatasetRequest(
    val metadata: DatasetMetadata,
    val fileName: String
)
