package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("MultiPolygon")
data class MultiPolygon @JvmOverloads constructor(
        val coordinates: List<List<List<List<Double>>>>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : Geometry
