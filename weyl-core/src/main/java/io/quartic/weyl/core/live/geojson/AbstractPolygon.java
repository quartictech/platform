package io.quartic.weyl.core.live.geojson;

import org.immutables.value.Value;

import java.util.List;

@MyStyle
@Value.Immutable
public interface AbstractPolygon extends Geometry {
    @Value.Parameter List<List<List<Double>>> coordinates();
}
