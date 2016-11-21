package io.quartic.weyl.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;

import java.util.Set;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = GeofenceViolationsUpdateMessageImpl.class)
@JsonDeserialize(as = GeofenceViolationsUpdateMessageImpl.class)
public interface GeofenceViolationsUpdateMessage extends SocketMessage {
    Set<EntityId> violatingGeofenceIds();
}
