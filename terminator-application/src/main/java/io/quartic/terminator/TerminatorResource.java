package io.quartic.terminator;

import io.quartic.model.LiveEvent;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/datasets")
public class TerminatorResource {
    @POST
    @Path("/{id}")
    public void postToDataset(@PathParam("id") String id, LiveEvent event) {
        // TODO: somehow make turn this into an Observable
    }
}
