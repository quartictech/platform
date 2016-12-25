package io.quartic.common.geojson

data class Feature @JvmOverloads constructor(
        val id: String? = null,
        val geometry: Geometry? = null,
        val properties: Map<String, Any> = emptyMap(),
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : GeoJsonObject