package io.quartic.weyl.core.live.geojson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @Type(value = Point.class, name = "Point"),
        @Type(value = LineString.class, name = "LineString"),
        @Type(value = Polygon.class, name = "Polygon")
})
public interface Geometry {
}
