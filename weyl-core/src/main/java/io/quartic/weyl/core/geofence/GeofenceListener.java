package io.quartic.weyl.core.geofence;

import io.quartic.weyl.core.model.AbstractFeature;

import java.util.Collection;

public interface GeofenceListener {
    void onViolationBegin(Violation violation);
    void onViolationEnd(Violation violation);
    void onGeometryChange(Collection<AbstractFeature> features);
}
