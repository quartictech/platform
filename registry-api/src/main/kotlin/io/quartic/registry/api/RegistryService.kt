package io.quartic.registry.api

import feign.Headers
import feign.Param
import feign.RequestLine
import io.quartic.registry.api.model.Customer
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("/")
interface RegistryService {
    @RequestLine("GET /customers?subdomain={subdomain}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomer(
        @Param("subdomain") @QueryParam("subdomain") subdomain: String?
    ): Customer
}
