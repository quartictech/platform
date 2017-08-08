package io.quartic.registry.api

import io.quartic.registry.api.model.Customer
import retrofit2.Call
import retrofit2.http.Query
import java.util.concurrent.CompletableFuture
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("/")
interface RegistryService {
    @GET
    @retrofit2.http.GET("customers")
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @Query("subdomain") @QueryParam("subdomain") subdomain: String?,
        @Query("githubRepoId") @QueryParam("githubRepoId") githubRepoId: Long?
    ): Call<Customer?>
}

@Path("/")
interface RegistryServiceAsync {
    @retrofit2.http.GET("/customers")
    fun getCustomerAsync(
        @Query("subdomain") subdomain: String?,
        @Query("githubRepoId") githubRepoId: Long?
    ): CompletableFuture<Customer?>
}
