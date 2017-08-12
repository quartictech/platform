package io.quartic.bild.resource

import com.google.common.base.Preconditions
import io.quartic.bild.store.JobResultStore
import io.quartic.bild.model.BildId
import org.apache.commons.io.IOUtils.copy
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/backchannel")
class BackChannelResource(private val jobResults: JobResultStore) {
    val resource = javaClass.getResource("/quarty.tar.gz")

    init {
        Preconditions.checkState(resource != null, "Runner distribution not found!")
    }

    @Path("/{jobId}")
    @POST
    fun backchannel(@PathParam("jobId") jobId: BildId, data: Any) {
        jobResults.putDag(jobId, data)
    }

    // TODO - this is gross, but given that we have this backchannel endpoint, it will suffice for now
    @Path("/runner")
    @Produces("application/gzip")
    @GET
    fun runner(): Response = Response.ok(StreamingOutput { ostream -> copy(resource.openStream(), ostream) }).build()
}
