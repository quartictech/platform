package io.quartic.weyl.core.alert;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;

import java.util.Set;

public class AlertProcessor {
    private final Set<AlertListener> listeners = Sets.newHashSet();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(new GeofenceListener() {
            @Override
            public void onViolation(Violation violation) {
                createAlert(Alert.of("Geofence violation", violation.message()));
            }

            @Override
            public void onGeometryChange(Geometry geometry) {
                // Do nothing
            }
        });
    }

    public synchronized void addListener(AlertListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(AlertListener listener) {
        listeners.remove(listener);
    }

    public synchronized void createAlert(AbstractAlert alert) {
        listeners.forEach(l -> l.onAlert(alert));
    }
}
