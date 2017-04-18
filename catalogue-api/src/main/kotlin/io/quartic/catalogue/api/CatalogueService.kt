package io.quartic.catalogue.api

import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/datasets")
interface CatalogueService {
    /**
     * Registers a dataset and assigns it a randomly-chosen [DatasetId].
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerDataset(config: DatasetConfig): DatasetId

    /**
     * Registers a dataset (or updates an existing dataset) with a specified [DatasetId].
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerOrUpdateDataset(@PathParam("id") id: DatasetId, config: DatasetConfig): DatasetId

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(): Map<DatasetId, DatasetConfig>

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDataset(@PathParam("id") id: DatasetId): DatasetConfig

    @DELETE
    @Path("/{id}")
    fun deleteDataset(@PathParam("id") id: DatasetId)
}
