package io.quartic.catalogue.api

import feign.Headers
import feign.Param
import feign.RequestLine
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
    @RequestLine("POST /datasets/{namespace}", decodeSlash=false)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @POST
    @Path("/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerDataset(
            @Param("namespace") @PathParam("namespace") namespace: DatasetNamespace,
            config: DatasetConfig
    ): DatasetCoordinates

    /**
     * Registers a dataset (or updates an existing dataset) in a specified namespace, with a specified [DatasetId].
     */
    @RequestLine("POST /datasets/{namespace}/{id}", decodeSlash=false)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @PUT
    @Path("/{namespace}/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun registerOrUpdateDataset(
            @Param("namespace") @PathParam("namespace") namespace: DatasetNamespace,
            @Param("id")        @PathParam("id") id: DatasetId,
            config: DatasetConfig
    ): DatasetCoordinates

    // TODO: get namespaces

    // In an ideal world this would be Map<DatasetCoordinates, DatasetConfig>, but can't have that as a key in JSON
    @RequestLine("GET /datasets", decodeSlash=false)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(): Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>

    @RequestLine("GET /datasets/{namespace}/{id}", decodeSlash=false)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @GET
    @Path("/{namespace}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDataset(
            @Param("namespace") @PathParam("namespace") namespace: DatasetNamespace,
            @Param("id")        @PathParam("id") id: DatasetId
    ): DatasetConfig

    @RequestLine("DELETE /datasets/{namespace}/{id}", decodeSlash=false)
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @DELETE
    @Path("/{namespace}/{id}")
    fun deleteDataset(
            @Param("namespace") @PathParam("namespace") namespace: DatasetNamespace,
            @Param("id")        @PathParam("id") id: DatasetId
    )
}
