package io.quartic.geojson

data class MultiPolygon @JvmOverloads constructor(
        val coordinates: List<List<List<List<Double>>>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry
