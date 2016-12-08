package io.quartic.weyl.core.alert;

import com.google.common.collect.Sets;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static io.quartic.weyl.core.geofence.Geofence.alertLevel;

public class AlertProcessor {
    public static final AttributeNameImpl ALERT_LEVEL = AttributeNameImpl.of("_alertLevel");
    private final Set<AlertListener> listeners = Sets.newHashSet();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(new GeofenceListener() {
            @Override
            public void onViolationBegin(Violation violation) {
                createAlert(AlertImpl.of(
                        "Geofence violation",
                        Optional.of(violation.message()),
                        alertLevel(violation.geofence().feature())
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
