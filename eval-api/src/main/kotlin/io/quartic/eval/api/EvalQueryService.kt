package io.quartic.eval.api

import io.quartic.common.client.ClientBuilder.Companion.Jaxable
import io.quartic.common.client.ClientBuilder.Companion.Retrofittable
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.ApiDag
import io.quartic.eval.api.model.Build
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.CompletableFuture
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Jaxable
@javax.ws.rs.Path("/query")
interface EvalQueryService {
    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/dag2/cytoscape/{customer_id}")
    fun getLatestDag(
        @PathParam("customer_id") customerId: CustomerId
    ): ApiDag

    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/dag2/cytoscape/{customer_id}/{build_number}")
    fun getDag(
        @PathParam("customer_id") customerId: CustomerId,
        @PathParam("build_number") buildNumber: Long
    ): ApiDag

    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/build/{customer_id}")
    fun getBuilds(
        @PathParam("customer_id") customerId: CustomerId
    ): List<Build>

    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/build/{customer_id}/{build_number}")
    fun getBuild(
        @PathParam("customer_id") customerId: CustomerId,
        @PathParam("build_number") buildNumber: Long
    ): Build

    @javax.ws.rs.GET
    @javax.ws.rs.Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/build/{customer_id}/{build_number}/events")
    fun getBuildEvents(
        @PathParam("customer_id") customerId: CustomerId,
        @PathParam("build_number") buildNumber: Long
    ): List<ApiBuildEvent>
}

@Retrofittable
interface EvalQueryServiceClient {
    @GET("query/dag/cytoscape/{customer_id}")
    fun getLatestDagAsync(
        @Path("customer_id") customerId: CustomerId
    ): CompletableFuture<ApiDag>

    @GET("query/dag/cytoscape/{customer_id}/{build_number}")
    fun getDagAsync(
        @Path("customer_id") customerId: CustomerId,
        @Path("build_number") buildNumber: Long
    ): CompletableFuture<ApiDag>

    @GET("query/build/{customer_id}")
    fun getBuildsAsync(
        @Path("customer_id") customerId: CustomerId
    ): CompletableFuture<List<Build>>

    @GET("query/build/{customer_id}/{build_number}")
    fun getBuildAsync(
        @Path("customer_id") customerId: CustomerId,
        @Path("build_number") buildNumber: Long
    ): CompletableFuture<Build>

    @GET("query/build/{customer_id}/{build_number}/events")
    fun getBuildEventsAsync(
        @Path("customer_id") customerId: CustomerId,
        @Path("build_number") buildNumber: Long
    ): CompletableFuture<List<ApiBuildEvent>>
}
