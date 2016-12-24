package io.quartic.geojson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        Type(value = Point::class, name = "Point"),
        Type(value = LineString::class, name = "LineString"),
        Type(value = Polygon::class, name = "Polygon"),
        Type(value = MultiPolygon::class, name = "MultiPolygon"),
        Type(value = MultiPoint::class, name = "MultiPoint"),
        Type(value = MultiLineString::class, name = "MultiLineString")
)
interface Geometry {
    // TODO
//    fun <T> accept(geometryVisitor: GeometryVisitor<T>): T
}
