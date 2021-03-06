package io.quartic.eval

import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.sequencer.BuildInitiator
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.SendChannel
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.MediaType

@Path("/")
class TriggerResource(private val buildInitiator: BuildInitiator, private val channel: SendChannel<BuildContext>) {
    @POST
    @Path("/trigger")
    @Produces(MediaType.APPLICATION_JSON)
    fun trigger(details: BuildTrigger, @Suspended response: AsyncResponse) {
        async(CommonPool) {
            val build = buildInitiator.start(details)

            if (build != null) {
                channel.send(build)
                response.resume(build.build.id)
            } else {
                response.resume(NotFoundException("No customer matching trigger"))
            }
        }
    }
}
