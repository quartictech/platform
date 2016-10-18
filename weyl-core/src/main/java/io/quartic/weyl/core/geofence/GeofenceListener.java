package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;

public interface GeofenceListener {
    void onViolation(Violation violation);
    void onGeometryChange(Geometry geometry);
}
