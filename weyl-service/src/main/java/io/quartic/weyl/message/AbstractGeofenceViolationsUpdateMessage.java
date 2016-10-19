package io.quartic.weyl.message;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.geofence.GeofenceId;
import org.immutables.value.Value;

import java.util.Set;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceViolationsUpdateMessage extends SocketMessage {
    Set<GeofenceId> violatingGeofenceIds();
}
