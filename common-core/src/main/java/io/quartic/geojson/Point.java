package io.quartic.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = PointImpl.class)
@JsonDeserialize(as = PointImpl.class)
public interface Point extends Geometry {
    List<Double> coordinates();

    @Override
    default <T> T accept(GeometryVisitor<T> geometryVisitor) {
        return geometryVisitor.visit(this);
    }
}
