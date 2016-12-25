package io.quartic.common.geojson

data class LineString @JvmOverloads constructor(
        val coordinates: List<List<Double>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry
