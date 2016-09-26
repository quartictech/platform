package io.quartic.weyl.core.geojson;

import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractPoint extends Geometry {
    List<Double> coordinates();
}
