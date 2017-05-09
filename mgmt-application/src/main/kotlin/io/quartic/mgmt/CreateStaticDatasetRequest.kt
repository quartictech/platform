package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.MimeTypes

data class CreateStaticDatasetRequest(
        val metadata: DatasetMetadata,
        val fileName: String,
        val fileType: FileType
) : CreateDatasetRequest {
    fun mimeType() = when (fileType) {
        FileType.CSV -> MimeTypes.CSV
        FileType.GEOJSON -> MimeTypes.GEOJSON
        FileType.RAW -> MimeTypes.RAW
    }
}
