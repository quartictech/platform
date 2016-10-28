package io.quartic.jester.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/datasets")
public interface JesterService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Collection<DatasetId> listDatasets();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    DatasetId registerDataset(DatasetConfig config);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    DatasetConfig getDataset(@PathParam("id") String id);

    @DELETE
    @Path("/{id}")
    void deleteDataset(@PathParam("id") String id);
}
