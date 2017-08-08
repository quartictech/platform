package io.quartic.registry.api

import io.quartic.registry.api.model.Customer
import retrofit2.Call
import retrofit2.http.Query
import java.util.concurrent.CompletableFuture
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import io.quartic.common.model.CustomerId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/")
interface RegistryService {
    // JAX-RS
    @GET
    @Path("/customers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerById(
        @retrofit2.http.Path("id") @PathParam("id") customerId: CustomerId
    ): Customer

    // JAX-RS
    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @Query("subdomain") @QueryParam("subdomain") subdomain: String?,
        @Query("githubRepoId") @QueryParam("githubRepoId") githubRepoId: Long?
    ): Customer
}

@Path("/")
interface RegistryServiceClient {
    // Retrofit
    @retrofit2.http.GET("customers/{id}")
    fun getCustomerById(
        @retrofit2.http.Path("id") @PathParam("id") customerId: CustomerId
    ): CompletableFuture<Customer>

    @retrofit2.http.GET("customers")
    fun getCustomer(
        @Query("subdomain") subdomain: String?,
        @Query("githubRepoId") githubRepoId: Long?
    ): CompletableFuture<Customer?>
}
