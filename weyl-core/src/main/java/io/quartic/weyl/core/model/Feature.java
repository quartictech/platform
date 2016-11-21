package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface Feature {
    EntityId entityId();
    Geometry geometry();
    Attributes attributes();
}
