package io.quartic.bild.resource

import io.quartic.bild.JobResultStore
import io.quartic.bild.model.CustomerId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/dag")
class DagResource(val pipelines: Map<String, Any>, val jobResults: JobResultStore) {
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    fun dag(@PathParam("customerId") customerId: CustomerId): Any? = jobResults.getLatest(customerId)
}

