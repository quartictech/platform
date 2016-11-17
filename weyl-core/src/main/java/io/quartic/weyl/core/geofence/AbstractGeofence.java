package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractAttributes;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
interface AbstractGeofence {
    GeofenceId id();
    GeofenceType type();
    Geometry geometry();
    AbstractAttributes attributes();
}
