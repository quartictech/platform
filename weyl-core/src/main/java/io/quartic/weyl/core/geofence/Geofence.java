package io.quartic.weyl.core.geofence;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
public interface Geofence {
    GeofenceType type();
    Feature feature();
}
