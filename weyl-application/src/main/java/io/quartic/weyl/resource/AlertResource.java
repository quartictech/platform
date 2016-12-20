package io.quartic.weyl.resource;

import io.quartic.weyl.core.model.Alert;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/alerts")
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {
    private PublishSubject<Alert> alerts = PublishSubject.create();

    @POST
    public void createAlert(Alert alert) {
        alerts.onNext(alert);
    }

    public Observable<Alert> alerts() {
        return alerts;
    };
}
