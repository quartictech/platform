package io.quartic.weyl.resource;

import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertProcessor;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/alerts")
@Consumes(MediaType.APPLICATION_JSON)
public class AlertResource {
    private final AlertProcessor processor;

    public AlertResource(AlertProcessor processor) {
        this.processor = processor;
    }

    @POST
    public void createAlert(Alert alert) {
        processor.createAlert(alert);
    }
}
