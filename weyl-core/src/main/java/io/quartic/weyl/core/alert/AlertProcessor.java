package io.quartic.weyl.core.alert;

import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AlertProcessor {
    public static final AttributeName ALERT_LEVEL = AttributeNameImpl.of("_alertLevel");
    private final PublishSubject<Alert> alerts = PublishSubject.create();

    public AlertProcessor(GeofenceViolationDetector geofenceViolationDetector) {
//        geofenceViolationDetector.addListener(new GeofenceListener() {
//            @Override
//            public void onViolationBegin(Violation violation) {
//                alerts.onNext(AlertImpl.of(
//                        "Geofence violation",
//                        Optional.of(String.format("Boundary violated by entity '%s'", violation.feature().entityId())),
//                        alertLevel(violation.geofence().feature())
//                ));
//            }
//
//            @Override
//            public void onViolationEnd(Violation violation) {
//                // Do nothing
//            }
//        });
    }

    public Observable<Alert> alerts() {
        return alerts;
    }
}
