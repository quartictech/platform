package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("Polygon")
data class Polygon(
        val coordinates: List<List<List<Double>>>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : Geometry
