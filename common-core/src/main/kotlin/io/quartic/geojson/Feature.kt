package io.quartic.geojson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("Feature")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
data class Feature(
        val id: String?,
        val geometry: Geometry?,
        val properties: Map<String, Any>,
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
)
