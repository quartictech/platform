package io.quartic.weyl.core.geofence;

import com.vividsolutions.jts.geom.Geometry;

import java.util.Collection;

public interface GeofenceListener {
    void onViolation(Violation violation);
    void onGeometryChange(Collection<Geometry> geometries);
}
