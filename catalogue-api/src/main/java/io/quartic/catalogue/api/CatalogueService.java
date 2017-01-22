package io.quartic.catalogue.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/datasets")
public interface CatalogueService {
    /**
     * Registers a dataset and assigns it a randomly-chosen {@link DatasetId}.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    DatasetId registerDataset(DatasetConfig config);

    /**
     * Registers a dataset (or updates an existing dataset) with a specified {@link DatasetId}.
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    DatasetId registerOrUpdateDataset(@PathParam("id") DatasetId id, DatasetConfig config);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<DatasetId, DatasetConfig> getDatasets();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    DatasetConfig getDataset(@PathParam("id") DatasetId id);

    @DELETE
    @Path("/{id}")
    void deleteDataset(@PathParam("id") DatasetId id);
}
