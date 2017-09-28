package io.quartic.catalogue.api

import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.client.ClientBuilder.Companion.Jaxable
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import retrofit2.http.*
import java.util.concurrent.CompletableFuture
import javax.ws.rs.core.MediaType

@Jaxable
@javax.ws.rs.Path("/datasets")
interface CatalogueService {
    /**
     * Registers a dataset in a specified namespace, and assigns it a randomly-chosen [DatasetId].
     */
    @javax.ws.rs.POST
    @javax.ws.rs.Path("/{namespace}")
    @javax.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    fun registerDataset(
        @javax.ws.rs.PathParam("namespace") namespace: DatasetNamespace,
        config: DatasetConfig
    ): DatasetCoordinates

    /**
     * Registers a dataset (or updates an existing dataset) in a specified namespace, with a specified [DatasetId].
     */
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @javax.ws.rs.PUT
    @javax.ws.rs.Path("/{namespace}/{id}")
    @javax.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    fun registerOrUpdateDataset(
        @javax.ws.rs.PathParam("namespace") namespace: DatasetNamespace,
        @javax.ws.rs.PathParam("id") id: DatasetId,
        config: DatasetConfig
    ): DatasetCoordinates

    // TODO: get namespaces

    // In an ideal world this would be Map<DatasetCoordinates, DatasetConfig>, but can't have that as a key in JSON
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(): Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>

    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @javax.ws.rs.GET
    @javax.ws.rs.Path("/{namespace}/{id}")
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    fun getDataset(
        @javax.ws.rs.PathParam("namespace") namespace: DatasetNamespace,
        @javax.ws.rs.PathParam("id") id: DatasetId
    ): DatasetConfig

    @Headers("Content-Type: " + MediaType.APPLICATION_JSON)
    @javax.ws.rs.DELETE
    @javax.ws.rs.Path("/{namespace}/{id}")
    fun deleteDataset(
        @javax.ws.rs.PathParam("namespace") namespace: DatasetNamespace,
        @javax.ws.rs.PathParam("id") id: DatasetId
    )
}

@Retrofittable
interface CatalogueClient {
    @POST("datasets/{namespace}")
    fun registerDatasetAsync(
        @Path("namespace") namespace: DatasetNamespace,
        @Body config: DatasetConfig
    ): CompletableFuture<DatasetCoordinates>

    @PUT("datasets/{namespace}/{id}")
    fun registerOrUpdateDatasetAsync(
        @Path("namespace") namespace: DatasetNamespace,
        @Path("id") id: DatasetId,
        @Body config: DatasetConfig
    ): CompletableFuture<DatasetCoordinates>

    @GET("datasets")
    fun getDatasetsAsync(): CompletableFuture<Map<DatasetNamespace, Map<DatasetId, DatasetConfig>>>

    @GET("datasets/{namespace}/{id}")
    fun getDatasetAsync(
        @Path("namespace") namespace: DatasetNamespace,
        @Path("id") id: DatasetId
    ): CompletableFuture<DatasetConfig>

    @DELETE("datasets/{namespace}/{id}")
    fun deleteDatasetAsync(
        @Path("namespace") namespace: DatasetNamespace,
        @Path("id") id: DatasetId
    ): CompletableFuture<Unit>
}
