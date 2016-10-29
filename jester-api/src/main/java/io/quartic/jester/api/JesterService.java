package io.quartic.jester.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/datasets")
public interface JesterService {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    DatasetId registerDataset(DatasetConfig config);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<DatasetId, DatasetConfig> getDatasets();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    DatasetConfig getDataset(@PathParam("id") String id);

    @DELETE
    @Path("/{id}")
    void deleteDataset(@PathParam("id") String id);
}
