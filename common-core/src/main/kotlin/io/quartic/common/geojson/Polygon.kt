package io.quartic.common.geojson

data class Polygon(
        val coordinates: List<List<List<Double>>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry
