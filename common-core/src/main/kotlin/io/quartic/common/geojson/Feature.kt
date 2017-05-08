package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("Feature")
data class Feature @JvmOverloads constructor(
        val id: String? = null,
        val geometry: Geometry? = null,
        val properties: Map<String, Any> = emptyMap(),
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : GeoJsonObject