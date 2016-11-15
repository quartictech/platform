package io.quartic.geojson;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractPolygon extends Geometry {
    List<List<List<Double>>> coordinates();
}
