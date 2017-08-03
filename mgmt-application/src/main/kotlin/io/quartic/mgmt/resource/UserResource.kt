package io.quartic.mgmt.resource

import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.mgmt.model.Profile
import java.net.URI
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@PermitAll
@Path("/profile")
class UserResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfile(@Auth user: User): Profile {
        // TODO - actually call Github API

        return Profile(
            user = user,
            name = "Johnny Noobhole",
            avatarUrl = URI("https://avatars3.githubusercontent.com/u/1058509?v=4")
        )
    }
}
