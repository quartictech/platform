package io.quartic.zeus.resource

import io.dropwizard.auth.Auth
import io.quartic.common.auth.legacy.LegacyUser
import io.quartic.zeus.model.SessionInfo
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/session-info")
class SessionInfoResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@Auth user: LegacyUser) = SessionInfo(user.name)
}

