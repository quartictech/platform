package io.quartic.bild.api

import feign.Headers
import feign.Param
import feign.RequestLine
import io.quartic.common.model.CustomerId
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/query")
interface BildQueryService {
    @RequestLine("GET /query/dag/{customerId}")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @GET
    @Path("/dag/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun dag(
        @Param("customerId") @PathParam("customerId") customerId: CustomerId
    ): Any
}
