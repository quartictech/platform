package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("GeometryCollection")
data class GeometryCollection @JvmOverloads constructor(
        val geometries: List<Geometry>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : Geometry
