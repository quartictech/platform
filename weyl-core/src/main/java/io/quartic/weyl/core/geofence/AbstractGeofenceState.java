package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
interface AbstractGeofenceState {
    boolean ok();
    String detail();
}
