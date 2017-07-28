package io.quartic.orf

import io.quartic.common.auth.JwtGenerator
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/authenticate")
class OrfResource(private val jwtGenerator: JwtGenerator) {
    // TODO: proper auth
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authenticate(userId: String) = AuthResponse(jwtGenerator.generate(userId))
}
