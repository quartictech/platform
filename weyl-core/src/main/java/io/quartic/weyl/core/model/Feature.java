package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface Feature {
    @Value.Parameter Optional<String> externalId();
    @Value.Parameter FeatureId uid();    // Must be unique

    @Value.Parameter Geometry geometry();
    @Value.Parameter Map<String, Object> metadata();
}
