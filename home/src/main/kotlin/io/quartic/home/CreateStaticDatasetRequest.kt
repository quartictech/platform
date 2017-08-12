package io.quartic.home

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

    fun extensions() = when (fileType) {
        FileType.GEOJSON -> mapOf<String, Any>(Pair("map", emptyMap<String, Any>()))
        else -> emptyMap()
    }
}
