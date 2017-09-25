package io.quartic.home.resource

import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.github.GitHubClient
import io.quartic.home.model.Profile
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.ServerErrorException
import javax.ws.rs.core.MediaType

@PermitAll
@Path("/profile")
class UserResource(private val gitHubApi: GitHubClient) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfile(@Auth user: User): Profile {
        // TODO - ultimately we should be doing this via a stored auth token, rather than storing the GH ID in the JWT

        val ghUser = try {
            gitHubApi.userAsync(user.id.toInt()).get()
        } catch (e: Exception) {
            throw ServerErrorException("GitHub communication error", 500, e)
        }
        return Profile(
            name = ghUser.name,
            avatarUrl = ghUser.avatarUrl
        )
    }
}
