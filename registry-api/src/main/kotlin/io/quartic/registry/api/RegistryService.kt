package io.quartic.registry.api

import io.quartic.common.client.Retrofittable
import io.quartic.common.model.CustomerId
import io.quartic.registry.api.model.Customer
import retrofit2.http.Query
import java.util.concurrent.CompletableFuture
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

// JAX-RS
@Path("/")
interface RegistryService {
    @GET
    @Path("/customers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerById(
        @PathParam("id") customerId: CustomerId
    ): Customer

    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @QueryParam("subdomain") subdomain: String?,
        @QueryParam("githubRepoId") githubRepoId: Long?
    ): Customer
}

@Retrofittable
interface RegistryServiceClient {
    @retrofit2.http.GET("customers/{id}")
    fun getCustomerByIdAsync(
        @retrofit2.http.Path("id") customerId: CustomerId
    ): CompletableFuture<Customer>

    @retrofit2.http.GET("customers")
    fun getCustomerAsync(
        @Query("subdomain") subdomain: String?,
        @Query("githubRepoId") githubRepoId: Long?
    ): CompletableFuture<Customer>
}
