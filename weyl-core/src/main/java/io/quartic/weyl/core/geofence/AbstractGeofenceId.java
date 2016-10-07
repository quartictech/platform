package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Uid;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractGeofenceId extends Uid {
}
