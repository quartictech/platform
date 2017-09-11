package io.quartic.eval

import io.quartic.eval.api.model.BuildTrigger
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.runBlocking
import javax.ws.rs.POST
import javax.ws.rs.Path

@Path("/")
class EvalResource(private val channel: SendChannel<BuildTrigger>) {
    @POST
    @Path("/trigger")
    fun trigger(details: BuildTrigger) = runBlocking { channel.send(details) }
}
