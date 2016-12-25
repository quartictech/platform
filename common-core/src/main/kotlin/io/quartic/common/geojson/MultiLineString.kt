package io.quartic.common.geojson

data class MultiLineString @JvmOverloads constructor(
        val coordinates: List<List<List<Double>>>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : Geometry
