package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetMetadata

data class CreateStaticDatasetRequest(
        val metadata: DatasetMetadata,
        val fileName: String,
        val fileType: FileType
) : CreateDatasetRequest
