package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractFeature {
    EntityId entityId();
    FeatureId uid();    // Must be unique
    Geometry geometry();
    Map<AttributeName, Object> attributes();
}
