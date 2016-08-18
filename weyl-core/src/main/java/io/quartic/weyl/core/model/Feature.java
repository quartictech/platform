package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public interface Feature {
    String id();

    Geometry geometry();

    Map<String, Object> metadata();
}
