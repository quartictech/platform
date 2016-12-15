package io.quartic.weyl.core.alert;

import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Feature;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.Optional;

import static io.quartic.weyl.core.geofence.Geofence.alertLevel;

public class AlertProcessor {
    public static final AttributeName ALERT_LEVEL = AttributeNameImpl.of("_alertLevel");
    private final PublishSubject<Alert> alerts = PublishSubject.create();

    public AlertProcessor(GeofenceStore geofenceStore) {
        geofenceStore.addListener(new GeofenceListener() {
            @Override
            public void onViolationBegin(Violation violation) {
                alerts.onNext(AlertImpl.of(
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

    public Observable<Alert> alerts() {
        return alerts;
    }
}
