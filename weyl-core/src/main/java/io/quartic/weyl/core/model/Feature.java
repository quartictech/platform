package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface Feature {
    String id();

    Geometry geometry();

    Map<String, Optional<Object>> metadata();
}
