package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("FeatureCollection")
data class FeatureCollection @JvmOverloads constructor(
        val features: List<Feature>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
) : GeoJsonObject
