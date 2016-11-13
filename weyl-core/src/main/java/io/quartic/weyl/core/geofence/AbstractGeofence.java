package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@SweetStyle
interface AbstractGeofence {
    GeofenceId id();
    GeofenceType type();
    Geometry geometry();
    Map<String, Object> metadata();
}
