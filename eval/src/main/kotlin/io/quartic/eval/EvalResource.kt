package io.quartic.eval

import io.quartic.eval.api.model.TriggerDetails
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.runBlocking
import javax.ws.rs.POST
import javax.ws.rs.Path

@Path("/")
class EvalResource(private val channel: SendChannel<TriggerDetails>) {
    @POST
    @Path("/trigger")
    fun trigger(details: TriggerDetails) = runBlocking { channel.send(details) }
}
