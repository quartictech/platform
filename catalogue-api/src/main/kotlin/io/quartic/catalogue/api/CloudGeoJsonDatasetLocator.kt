package io.quartic.catalogue.api

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.quartic.common.SweetStyle
import org.immutables.value.Value

data class CloudGeoJsonDatasetLocator(val path: String, val streaming: Boolean = false) : DatasetLocator
