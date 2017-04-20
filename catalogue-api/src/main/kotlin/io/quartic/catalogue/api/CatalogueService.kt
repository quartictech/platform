package io.quartic.catalogue.api

import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/datasets")
interface CatalogueService {
    /**
     * Registers a dataset in a specified namespace, and assigns it a randomly-chosen [DatasetId].
     */
    @POST
    @Path("/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerDataset(
            @PathParam("namespace") namespace: DatasetNamespace,
            config: DatasetConfig
    ): DatasetCoordinates

    /**
     * Registers a dataset (or updates an existing dataset) in a specified namespace, with a specified [DatasetId].
     */
    @PUT
    @Path("/{namespace}/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerOrUpdateDataset(
            @PathParam("namespace") namespace: DatasetNamespace,
            @PathParam("id") id: DatasetId,
            config: DatasetConfig
    ): DatasetCoordinates

    // TODO: get namespaces

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(): Map<DatasetCoordinates, DatasetConfig>

    @GET
    @Path("/{namespace}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDataset(
            @PathParam("namespace") namespace: DatasetNamespace,
            @PathParam("id") id: DatasetId
    ): DatasetConfig

    @DELETE
    @Path("/{namespace}/{id}")
    fun deleteDataset(
            @PathParam("namespace") namespace: DatasetNamespace,
            @PathParam("id") id: DatasetId
    )
}
