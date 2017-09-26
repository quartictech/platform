package io.quartic.eval

import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.sequencer.BuildBootstrap
import io.quartic.eval.sequencer.BuildBootstrap.BuildContext
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.SendChannel
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.WebApplicationException
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended

@Path("/")
class EvalResource(private val buildBootstrap: BuildBootstrap, private val channel: SendChannel<BuildContext>) {
    @POST
    @Path("/trigger")
    fun trigger(details: BuildTrigger, @Suspended response: AsyncResponse) = async(CommonPool) {
        val build = buildBootstrap.start(details)

        if (build != null) {
            channel.send(build)
            response.resume(build.build.id)
        } else {
            response.resume(WebApplicationException("No customer matching trigger"))
        }
    }
}
