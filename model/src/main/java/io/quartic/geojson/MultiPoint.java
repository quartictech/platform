package io.quartic.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = MultiPointImpl.class)
@JsonDeserialize(as = MultiPointImpl.class)
public interface MultiPoint extends Geometry {
    List<List<Double>> coordinates();


    @Override
    default <T> T accept(GeometryVisitor<T> geometryVisitor) {
        return geometryVisitor.visit(this);
    }
}
