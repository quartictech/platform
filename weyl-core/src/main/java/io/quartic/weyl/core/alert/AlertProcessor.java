package io.quartic.weyl.core.alert;

import com.google.common.collect.Sets;
import io.quartic.weyl.core.alert.Alert.Level;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class AlertProcessor {
    public static final AttributeNameImpl GEOFENCE_LEVEL = AttributeNameImpl.of("_geofenceLevel");
    private final Set<AlertListener> listeners = Sets.newHashSet();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(new GeofenceListener() {
            @Override
            public void onViolationBegin(Violation violation) {
                createAlert(AlertImpl.of(
                        "Geofence violation",
                        Optional.of(violation.message()),
                        alertLevel(violation)
                ));
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

    private Alert.Level alertLevel(Violation violation) {
        final Object level = violation.geofence().feature().attributes().attributes().get(GEOFENCE_LEVEL);
        try {
            return Level.valueOf(level.toString().toUpperCase());
        } catch (Exception e) {
            return Level.SEVERE;    // Default
        }
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
