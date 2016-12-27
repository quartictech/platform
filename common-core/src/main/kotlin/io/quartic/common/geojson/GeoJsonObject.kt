package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
interface GeoJsonObject {
    val crs: Map<String, Any>?
    val bbox: List<Double>?
}