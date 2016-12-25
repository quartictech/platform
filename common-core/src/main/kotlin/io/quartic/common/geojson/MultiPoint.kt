package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("MultiPoint")
data class MultiPoint @JvmOverloads constructor(
        val coordinates: List<List<Double>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry