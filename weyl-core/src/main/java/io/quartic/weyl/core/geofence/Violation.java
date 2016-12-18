package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.EntityId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface Violation {
    EntityId entityId();
    EntityId geofenceId();
    Alert.Level level();
}
