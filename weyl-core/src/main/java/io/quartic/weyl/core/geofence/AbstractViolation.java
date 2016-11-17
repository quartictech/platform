package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractAttributes;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    ViolationId id();
    GeofenceId geofenceId();
    EntityId entityId();
    AbstractAttributes featureAttributes();
    AbstractAttributes geofenceAttributes();
    String message();
}
