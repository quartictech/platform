package io.quartic.common.geojson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type

@JsonSubTypes(
        Type(value = Point::class),
        Type(value = LineString::class),
        Type(value = Polygon::class),
        Type(value = MultiPoint::class),
        Type(value = MultiLineString::class),
        Type(value = MultiPolygon::class),
        Type(value = GeometryCollection::class)
)
interface Geometry : GeoJsonObject
