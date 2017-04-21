package io.quartic.catalogue.api.model

data class CloudGeoJsonDatasetLocator(val path: String, val streaming: Boolean = false) : DatasetLocator
