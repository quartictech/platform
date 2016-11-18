package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;

import java.util.Set;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceViolationsUpdateMessage extends SocketMessage {
    Set<EntityId> violatingGeofenceIds();
}
