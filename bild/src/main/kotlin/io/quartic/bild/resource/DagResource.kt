package io.quartic.bild.resource

import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/dag")
class DagResource(val pipelines: Map<String, Any>) {

    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    fun dag(@PathParam("customerId") customerId: String): Any? =
        pipelines.getOrElse(customerId, { throw WebApplicationException(404) })
}

