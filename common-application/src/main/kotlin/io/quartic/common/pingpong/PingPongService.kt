package io.quartic.common.pingpong

import feign.RequestLine
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/ping")
@Produces(MediaType.APPLICATION_JSON)
interface PingPongService {
    @RequestLine("GET /ping")
    @GET
    fun ping(): Pong
}
