package io.quartic.home.resource

import com.google.common.hash.Hashing
import io.quartic.common.auth.extractSubdomain
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.frontend.FrontendTokenGenerator
import io.quartic.common.auth.frontend.FrontendUser
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.uid.Uid
import io.quartic.common.uid.secureRandomGenerator
import io.quartic.github.AuthToken
import io.quartic.github.GitHubClient
import io.quartic.github.GitHubOAuthClient
import io.quartic.github.oauthUrl
import io.quartic.home.CookiesConfiguration
import io.quartic.home.GithubConfiguration
import io.quartic.registry.api.RegistryServiceClient
import org.apache.http.client.utils.URIBuilder
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.ResponseBuilder


@Path("/auth")
class AuthResource(
    private val gitHubConfig: GithubConfiguration,
    private val cookiesConfig: CookiesConfiguration,
    private val secretsCodec: SecretsCodec,
    private val tokenGenerator: FrontendTokenGenerator,
    private val registry: RegistryServiceClient,
    private val gitHubOAuth: GitHubOAuthClient,
    private val gitHubApi: GitHubClient
) {
    class NonceId(uid: String) : Uid(uid)

    private val LOG by logger()

    private val nonceGenerator = secureRandomGenerator(::NonceId)

    @GET
    @Path("/gh")
    fun github(@HeaderParam(HttpHeaders.HOST) host: String): Response {
        val nonce = nonceGenerator.get().uid

        val uri = oauthUrl(
            gitHubConfig.oauthApiRoot,
            gitHubConfig.clientId,
            "${gitHubConfig.trampolineUrl}/${extractSubdomain(host)}",
            gitHubConfig.scopes,
            nonce
        )

        return Response.temporaryRedirect(uri)
            .cookie(NONCE_COOKIE, hash(nonce), DEFAULT_MAX_AGE) // Session expiration
            .build()
    }

    @GET
    @Path("/gh/callback/{issuer}")
    fun githubCallback(
        @PathParam("issuer") issuer: String,
        @QueryParam("code") code: String?,
        @QueryParam("state") state: String?
    ): Response {
        // We're not an open redirector (due to formatted target), so it's fine to do no validation here.
        val uri = URIBuilder(gitHubConfig.redirectHost.format(issuer))
            .setPath("/login")
            .setParameter("provider", "gh")
            .setParameter("code", code.nonNull("code"))
            .setParameter("state", state.nonNull("state"))
            .build()
        return Response.temporaryRedirect(uri).build()
    }

    @POST
    @Path("/gh/complete")
    fun githubComplete(
        @QueryParam("code") code: String?,
        @QueryParam("state") state: String?,
        @CookieParam(NONCE_COOKIE) nonceCookie: String?,
        @HeaderParam(HttpHeaders.HOST) host: String
    ): Response {
        if (hash(state.nonNull("state")) != nonceCookie.nonNull("cookie")) {
            LOG.warn("Nonce hash mismatch")
            throw NotAuthorizedException("Authorisation failure")
        }

        val accessToken = callServerOrThrow { getAccessToken(code.nonNull("code")) }
        if (accessToken.accessToken == null) {
            LOG.error("Error while authorising against GitHub: ${accessToken.error} - ${accessToken.errorDescription}")
            throw NotAuthorizedException("GitHub authorisation failure")
        }

        val ghUser = callServerOrThrow { gitHubApi.userAsync(AuthToken(accessToken.accessToken!!)).get() }
        val ghOrgs = callServerOrThrow { gitHubApi.organizationsAsync(AuthToken(accessToken.accessToken!!)).get() }

        val subdomain = extractSubdomain(host)
        val customer = callServerOrThrow { registry.getCustomerAsync(subdomain, null).get()!! } // Should never be null

        if (ghOrgs.any { it.id == customer.githubOrgId }) {
            val user = FrontendUser(ghUser.id, customer.id) // TODO - using the GH ID as user ID is wrong in the long run
            val tokens = tokenGenerator.generate(user, subdomain)
            return Response.ok()
                .header(XSRF_TOKEN_HEADER, tokens.xsrf)
                .cookie(TOKEN_COOKIE, tokens.jwt, cookiesConfig.maxAgeSeconds)
                .build()
        } else {
            LOG.warn("User doesn't belong to organisation (${customer.githubOrgId} not in ${ghOrgs.map { "${it.id} (${it.login})" }})")
            throw NotAuthorizedException("User doesn't belong to organisation")
        }
    }

    private fun <R> callServerOrThrow(block: () -> R) = try {
        block()
    } catch (wba: WebApplicationException) {
        throw wba
    } catch (e: Exception) {
        throw ServerErrorException("Inter-service communication error", 500, e)
    }

    private fun getAccessToken(code: String) = gitHubOAuth.accessTokenAsync(
        gitHubConfig.clientId,
        secretsCodec.decrypt(gitHubConfig.clientSecretEncrypted).veryUnsafe,
        gitHubConfig.trampolineUrl.toString(),
        code
    ).get()

    // TODO - can we get DW to deal with this for QueryParam and CookieParam?
    private fun <T> T?.nonNull(name: String): T = this ?: throw BadRequestException("Missing parameter: $name")

    private fun ResponseBuilder.cookie(name: String, value: String, maxAgeSeconds: Int) = this.cookie(NewCookie(
        name,
        value,
        "/",
        null,
        null,
        maxAgeSeconds,
        cookiesConfig.secure,
        true    // httpOnly
    ))

    private fun hash(token: String) = Hashing.sha1().hashString(token, Charsets.UTF_8).toString()

    companion object {
        const val NONCE_COOKIE = "nonce"
    }
}
