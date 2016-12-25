package io.quartic.geojson

data class MultiLineString @JvmOverloads constructor(
        val coordinates: List<List<List<Double>>>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : Geometry
