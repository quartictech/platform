package io.quartic.catalogue.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatasetConfig(
    val metadata: DatasetMetadata,
    val locator: DatasetLocator,
    val extensions: Map<String, Any> = emptyMap()
)
