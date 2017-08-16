package io.quartic.bild.resource

import com.google.common.base.Preconditions
import io.quartic.bild.model.BuildId
import io.quartic.bild.api.model.Dag
import org.apache.commons.io.IOUtils.copy
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import io.quartic.bild.store.JobStore

@Path("/backchannel")
class BackChannelResource(private val jobStore: JobStore) {
    val resource = javaClass.getResource("/quarty.tar.gz")

    init {
        Preconditions.checkState(resource != null, "Runner distribution not found!")
    }

    @Path("/{buildId}")
    @POST
    fun backchannel(@PathParam("buildId") buildId: BuildId, data: Dag) {
        jobStore.setDag(buildId, data)
    }

    // TODO - this is gross, but given that we have this backchannel endpoint, it will suffice for now
    @Path("/runner")
    @Produces("application/gzip")
    @GET
    fun runner(): Response = Response.ok(StreamingOutput { ostream -> copy(resource.openStream(), ostream) }).build()
}
