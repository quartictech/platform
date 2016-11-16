package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@SweetStyle
interface AbstractGeofence {
    GeofenceId id();
    GeofenceType type();
    Geometry geometry();
    Map<AttributeName, Object> attributes();
}
