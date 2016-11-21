package io.quartic.geojson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
        @Type(value = PointImpl.class, name = "Point"),
        @Type(value = LineStringImpl.class, name = "LineString"),
        @Type(value = PolygonImpl.class, name = "Polygon"),
        @Type(value = MultiPolygonImpl.class, name = "MultiPolygon")
})
public interface Geometry {
    @Value.Parameter(false)
    Optional<Map<String, Object>> crs();
}
