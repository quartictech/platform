package io.quartic.weyl.core.alert;

import com.google.common.collect.Sets;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Set;

public class AlertProcessor {
    private final Set<AlertListener> listeners = Sets.newHashSet();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(new GeofenceListener() {
            @Override
            public void onViolationBegin(Violation violation) {
                createAlert(AlertImpl.of("Geofence violation", violation.message()));
            }

            @Override
            public void onViolationEnd(Violation violation) {
                // Do nothing
            }

            @Override
            public void onGeometryChange(Collection<Feature> features) {
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

    public synchronized void createAlert(Alert alert) {
        listeners.forEach(l -> l.onAlert(alert));
    }
}
