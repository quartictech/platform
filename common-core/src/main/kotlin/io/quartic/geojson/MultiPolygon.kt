package io.quartic.geojson

data class MultiPolygon(
        val coordinates: List<List<List<List<Double>>>>,
        // TODO
        val crs: Map<String, Any>?,
        val bbox: List<Double>?
) : Geometry
