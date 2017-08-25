package io.quartic.eval.api

import feign.Headers
import feign.RequestLine
import io.quartic.common.client.Jaxable
import io.quartic.eval.api.model.TriggerDetails
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType

@Jaxable
@Path("/trigger")
interface EvalTriggerService {
    @RequestLine("POST /trigger")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun trigger(trigger: TriggerDetails)
}
