package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

@Value.Immutable
public interface GeometryWithAttributes {
    @Value.Parameter Geometry geometry();
    @Value.Parameter AbstractAttributes attributes();
}
