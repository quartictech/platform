package io.quartic.terminator.api;

import io.quartic.geojson.FeatureCollection;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

@Path("/datasets")
public interface TerminatorService {
    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void postToDataset(@PathParam("id") String id, FeatureCollection featureCollection);
}
