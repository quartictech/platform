package io.quartic.registry.api

import feign.Headers
import feign.Param
import feign.RequestLine
import io.quartic.common.model.CustomerId
import io.quartic.registry.api.model.Customer
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/")
interface RegistryService {
    @RequestLine("GET /customers/{id}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @GET
    @Path("/customers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerById(
        @Param("id") @PathParam("id") customerId: CustomerId
    ): Customer

    @RequestLine("GET /customers?subdomain={subdomain}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @Param("subdomain") @QueryParam("subdomain") subdomain: String?
    ): Customer
}
