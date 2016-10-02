package io.quartic.weyl.core.alert;

import com.google.common.collect.Sets;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;

import java.util.Set;

public class AlertProcessor {
    private final Set<AlertListener> listeners = Sets.newHashSet();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(this::handleViolation);
    }

    private void handleViolation(Violation v) {
        createAlert(Alert.of("Geofence violation", v.message()));
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
