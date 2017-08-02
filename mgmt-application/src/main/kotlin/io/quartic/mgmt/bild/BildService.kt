package io.quartic.mgmt.bild

import feign.Headers
import feign.Param
import feign.RequestLine
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

// TODO - delete
interface BildService {
    @RequestLine("GET /dag/{customerId}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @GET
    @Path("/dag/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDag(
        @Param("customerId") @PathParam("customerId") customerId: String
    ): Any
}
