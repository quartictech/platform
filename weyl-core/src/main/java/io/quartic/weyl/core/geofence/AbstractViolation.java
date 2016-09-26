package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    ViolationId id();
    String message();
}
