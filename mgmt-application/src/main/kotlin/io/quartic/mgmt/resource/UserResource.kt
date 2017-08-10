package io.quartic.mgmt.resource

import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.common.client.client
import io.quartic.mgmt.GithubConfiguration
import io.quartic.mgmt.model.Profile
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.ServerErrorException
import javax.ws.rs.core.MediaType
import io.quartic.github.GitHub

@PermitAll
@Path("/profile")
class UserResource(
    private val gitHubConfig: GithubConfiguration,
    private val gitHubApi: GitHub = client<GitHub>(UserResource::class.java, gitHubConfig.apiRoot)
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfile(@Auth user: User): Profile {
        // TODO - ultimately we should be doing this via a stored auth token, rather than storing the GH ID in the JWT

        val ghUser = try {
            gitHubApi.user(user.id.toInt())
        } catch (e: Exception) {
            throw ServerErrorException("GitHub communication error", 500, e)
        }
        return Profile(
            name = ghUser.name,
            avatarUrl = ghUser.avatarUrl
        )
    }
}
