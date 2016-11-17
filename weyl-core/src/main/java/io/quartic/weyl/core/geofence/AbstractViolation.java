package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractViolation {
    ViolationId id();
    GeofenceId geofenceId();
    EntityId entityId();
    Map<AttributeName, Object> featureAttributes();
    Map<AttributeName, Object> geofenceAttributes();
    String message();
}
