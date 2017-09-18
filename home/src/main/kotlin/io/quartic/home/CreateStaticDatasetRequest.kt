package io.quartic.home

import io.quartic.catalogue.api.model.DatasetMetadata

data class CreateStaticDatasetRequest(
    val metadata: DatasetMetadata,
    val fileName: String
) : CreateDatasetRequest
