package io.quartic.weyl.core.geojson;

import org.immutables.value.Value;

import java.util.List;

@MyStyle
@Value.Immutable
public interface AbstractLineString extends Geometry {
    @Value.Parameter List<List<Double>> coordinates();
}
