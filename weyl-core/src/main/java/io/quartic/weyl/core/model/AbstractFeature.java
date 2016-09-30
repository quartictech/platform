package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface AbstractFeature {
    FeatureId id();
    Geometry geometry();
    Map<String, Optional<Object>> metadata();
}
