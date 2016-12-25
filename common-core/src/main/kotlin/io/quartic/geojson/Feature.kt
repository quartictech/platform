package io.quartic.geojson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("Feature")
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
data class Feature @JvmOverloads constructor(
        val id: String? = null,
        val geometry: Geometry? = null,
        val properties: Map<String, Any> = emptyMap(),
        // TODO
        val crs: Map<String, Any>? = emptyMap(),
        val bbox: List<Double>? = emptyList()
)