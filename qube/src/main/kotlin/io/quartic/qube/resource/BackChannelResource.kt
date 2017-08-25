package io.quartic.qube.resource

import com.google.common.base.Preconditions
import org.apache.commons.io.IOUtils.copy
import javax.ws.rs.*
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import io.quartic.qube.store.JobStore

@Path("/backchannel")
class BackChannelResource {
    val resource = javaClass.getResource("/quarty.tar.gz")

    init {
        Preconditions.checkState(resource != null, "Runner distribution not found!")
    }

    // TODO - this is gross, but given that we have this backchannel endpoint, it will suffice for now
    @Path("/runner")
    @Produces("application/gzip")
    @GET
    fun runner(): Response = Response.ok(StreamingOutput { ostream -> copy(resource.openStream(), ostream) }).build()
}
