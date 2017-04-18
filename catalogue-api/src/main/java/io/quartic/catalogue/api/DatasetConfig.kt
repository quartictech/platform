package io.quartic.catalogue.api

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatasetConfig(
    val metadata: DatasetMetadata,
    val locator: DatasetLocator,
    @get:JsonAnyGetter
    val extensions: Map<String, Any>
)
