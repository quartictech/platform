package io.quartic.geojson

data class MultiPoint(
        val coordinates: List<List<Double>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry