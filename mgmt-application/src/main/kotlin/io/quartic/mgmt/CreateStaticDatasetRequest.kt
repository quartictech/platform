package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.MimeType

data class CreateStaticDatasetRequest(
        val metadata: DatasetMetadata,
        val fileName: String,
        val fileType: FileType
) : CreateDatasetRequest {
    fun mimeType() = when (fileType) {
        FileType.CSV -> MimeType.CSV
        FileType.GEOJSON -> MimeType.GEOJSON
        FileType.RAW -> MimeType.RAW
    }
}
