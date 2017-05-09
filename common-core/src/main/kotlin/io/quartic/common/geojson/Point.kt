package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("Point")
data class Point @JvmOverloads constructor(
        val coordinates: List<Double>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : Geometry {
    constructor(vararg coordinates: Double) : this(coordinates.asList())
}
