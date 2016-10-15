package io.quartic.weyl.core.geojson;

import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractMultiPolygon extends Geometry {
    List<List<List<List<Double>>>> coordinates();
}
