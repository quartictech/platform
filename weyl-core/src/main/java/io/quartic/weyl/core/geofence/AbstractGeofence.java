package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

@Value.Immutable
@SweetStyle
interface AbstractGeofence {
    GeofenceType type();
    Geometry geometry();
}
