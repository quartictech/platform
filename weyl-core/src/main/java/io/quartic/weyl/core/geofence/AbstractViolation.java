package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    ViolationId id();
    GeofenceId geofenceId();
    String featureExternalId();
    String message();
}
