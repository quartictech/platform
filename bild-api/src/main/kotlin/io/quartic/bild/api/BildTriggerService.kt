package io.quartic.bild.api

import feign.Headers
import feign.RequestLine
import io.quartic.bild.api.model.TriggerDetails
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType

@Path("/trigger")
interface BildTriggerService {
    @RequestLine("POST /trigger")
    @Headers("Content-Type: ${MediaType.APPLICATION_JSON}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun trigger(trigger: TriggerDetails): Unit
}
