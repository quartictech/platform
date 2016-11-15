package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public interface GeometryWithMetadata {
    @Value.Parameter Geometry geometry();
    @Value.Parameter Map<AttributeName, Object> metadata();
}
