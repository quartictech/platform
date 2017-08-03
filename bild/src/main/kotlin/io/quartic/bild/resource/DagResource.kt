package io.quartic.bild.resource

import io.quartic.bild.JobResultStore
import io.quartic.bild.model.CustomerId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/dag")
class DagResource(val jobResults: JobResultStore, val defaultPipeline: Any) {
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    fun dag(@PathParam("customerId") customerId: CustomerId): Any = jobResults.getLatest(customerId)?.dag ?: defaultPipeline
}

