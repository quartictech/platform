package io.quartic.weyl.core.geofence;

public interface GeofenceListener {
    void onViolationBegin(Violation violation);
    void onViolationEnd(Violation violation);
}
