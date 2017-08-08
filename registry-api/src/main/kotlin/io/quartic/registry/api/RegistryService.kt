package io.quartic.registry.api

import io.quartic.registry.api.model.Customer
import retrofit2.Call
import retrofit2.http.Query
import java.util.concurrent.CompletableFuture
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import feign.Headers
import feign.RequestLine
import io.quartic.common.model.CustomerId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/")
interface RegistryService {
    // Feign
    @RequestLine("GET /customers/{id}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    // Retrofit
    @retrofit2.http.GET("customers")
    // JAX-RS
    @GET
    @Path("/customers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerById(
        @retrofit2.http.Path("id") @PathParam("id") customerId: CustomerId
    ): Customer

    // Feign
    @RequestLine("GET /customers?subdomain={subdomain}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    // Retrofit
    @retrofit2.http.GET("customers")
    // JAX-RS
    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @Query("subdomain") @QueryParam("subdomain") subdomain: String?,
        @Query("githubRepoId") @QueryParam("githubRepoId") githubRepoId: Long?
    ): Call<Customer?>
}

@Path("/")
interface RegistryServiceAsync {
    @retrofit2.http.GET("customers")
    fun getCustomerAsync(
        @Query("subdomain") subdomain: String?,
        @Query("githubRepoId") githubRepoId: Long?
    ): CompletableFuture<Customer?>
}
