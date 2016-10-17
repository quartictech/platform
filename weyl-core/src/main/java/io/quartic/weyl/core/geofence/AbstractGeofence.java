package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
interface AbstractGeofence {
    GeofenceId id();
    GeofenceType type();
    Geometry geometry();
}
