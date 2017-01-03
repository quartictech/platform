package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("FeatureCollection")
data class FeatureCollection @JvmOverloads constructor(
        val features: List<Feature>,
        override val crs: Map<String, Any>? = null,
        override val bbox: List<Double>? = null
) : GeoJsonObject
