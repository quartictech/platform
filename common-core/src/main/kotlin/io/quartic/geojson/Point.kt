package io.quartic.geojson

data class Point(
        val coordinates: List<Double>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry
