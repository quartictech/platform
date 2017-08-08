package io.quartic.bild.resource

import io.quartic.bild.JobResultStore
import io.quartic.bild.model.BildId
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam

@Path("/backchannel")
class BackChannelResource(private val jobResults: JobResultStore) {
    @Path("/{jobId}")
    @POST
    fun backchannel(@PathParam("jobId") jobId: BildId, data: Any) {
        jobResults.putDag(jobId, data)
    }
}
