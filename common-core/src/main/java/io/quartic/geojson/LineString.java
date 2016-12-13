package io.quartic.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = LineStringImpl.class)
@JsonDeserialize(as = LineStringImpl.class)
public interface LineString extends Geometry {
    List<List<Double>> coordinates();

    @Override
    default <T> T accept(GeometryVisitor<T> geometryVisitor) {
       return geometryVisitor.visit(this);
    }
}
