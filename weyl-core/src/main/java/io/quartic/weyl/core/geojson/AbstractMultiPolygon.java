package io.quartic.weyl.core.geojson;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractMultiPolygon extends Geometry {
    List<List<List<List<Double>>>> coordinates();
}
