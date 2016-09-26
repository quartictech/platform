package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface Feature {
    @Value.Parameter String id();

    @Value.Parameter Geometry geometry();

    @Value.Parameter Map<String, Optional<Object>> metadata();
}
