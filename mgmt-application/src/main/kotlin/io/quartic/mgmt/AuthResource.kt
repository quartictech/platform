package io.quartic.mgmt

import com.google.common.hash.Hashing
import feign.FeignException
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.getIssuer
import io.quartic.common.client.client
import io.quartic.common.logging.logger
import io.quartic.common.uid.Uid
import io.quartic.common.uid.secureRandomGenerator
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.UNAUTHORIZED


@Path("/auth")
class AuthResource(private val gitHubConfig: GithubConfiguration,
                   private val cookiesConfig: CookiesConfiguration,
                   private val tokenGenerator: TokenGenerator,
                   private val gitHubOAuth: GitHubOAuth = client<GitHubOAuth>(AuthResource::class.java, gitHubConfig.oauthApiRoot),
                   private val gitHubApi: GitHub = client<GitHub>(AuthResource::class.java, gitHubConfig.apiRoot)) {

    class NonceId(uid: String) : Uid(uid)

    private val LOG by logger()

    private val nonceGenerator = secureRandomGenerator(::NonceId)

    @GET
    @Path("/gh")
    fun github(@HeaderParam(HttpHeaders.HOST) host: String): Response? {
        val nonce = nonceGenerator.get().uid

        val uri = oauthUrl(
            gitHubConfig.oauthApiRoot,
            gitHubConfig.clientId,
            "${gitHubConfig.trampolineUrl}/${getIssuer(host)}",
            gitHubConfig.scopes,
            nonce
        )

        return Response.temporaryRedirect(uri)
            .cookie(cookie(NONCE_COOKIE, hash(nonce), DEFAULT_MAX_AGE)) // Session expiration
            .build()
    }

    @GET
    @Path("/gh/callback/{issuer}")
    fun githubCallback(
        @PathParam("issuer") issuer: String,
        @QueryParam("code") code: String?,
        @QueryParam("state") state: String?
    ): Response {
        fun String.urlEncode() = URLEncoder.encode(this, UTF_8.name())

        // We're not an open redirector (due to formatted target), so it's fine to do no validation here
        // We can't use UriBuilder or the like, because these aren't real query params, they're for react-router
        val uri = URI.create(
            "${gitHubConfig.redirectHost.format(issuer)}/#/login?" +
            "provider=gh&" +
            "code=${code.nonNull("code").urlEncode()}&" +
            "state=${state.nonNull("state").urlEncode()}"
        )
        return Response.temporaryRedirect(uri).build()
    }

    @POST
    @Path("/gh/complete")
    fun githubComplete(
        @QueryParam("code") code: String?,
        @QueryParam("state") state: String?,
        @CookieParam(NONCE_COOKIE) nonceCookie: String?,
        @HeaderParam(HttpHeaders.HOST) host: String,
        @Suspended response: AsyncResponse
    ) {
        if (hash(state.nonNull("state")) != nonceCookie.nonNull(NONCE_COOKIE)) {
            LOG.warn("Nonce hash mismatch")
            response.resume(Response.status(UNAUTHORIZED).build())
        }

        try {
            val accessToken = getAccessToken(code.nonNull("code"))

            if (accessToken.accessToken == null) {
                LOG.error("Exception while authorising against GitHub: ${accessToken.error} - ${accessToken.errorDescription}")
                response.resume(Response.status(401).build())
                return
            } else {
                val user = gitHubApi.user(accessToken.accessToken)
                val organizations = gitHubApi.organizations(accessToken.accessToken).map { org -> org.login }

                if (!organizations.intersect(gitHubConfig.allowedOrganisations).isEmpty()) {
                    val tokens = tokenGenerator.generate(user.login, getIssuer(host))
                    response.resume(Response.ok()
                        .header(XSRF_TOKEN_HEADER, tokens.xsrf)
                        .cookie(cookie(TOKEN_COOKIE, tokens.jwt, cookiesConfig.maxAgeSeconds))
                        .build())
                } else {
                    LOG.info("user ${user} denied access")
                    response.resume(Response.status(401).build())
                }
            }
        }
        catch (e: FeignException) {
            LOG.error("Exception communicating with GitHub", e)
            if (e.status() in 400..499) {
                response.resume(Response.status(UNAUTHORIZED).build())
            }
            else {
                response.resume(Response.serverError().build())
            }
        }
        catch (e: Exception) {
            LOG.error("Exception while authenticating", e)
            response.resume(Response.status(500).build())
        }
    }

    private fun getAccessToken(code: String) = gitHubOAuth.accessToken(
        gitHubConfig.clientId,
        gitHubConfig.clientSecret,
        gitHubConfig.trampolineUrl,
        code
    )

    // TODO - can we get DW to deal with this for QueryParam and CookieParam?
    private fun <T> T?.nonNull(name: String): T = this ?: throw BadRequestException("Missing parameter: $name")

    private fun cookie(name: String, value: String, maxAgeSeconds: Int): NewCookie {
        return NewCookie(
            name,
            value,
            "/",
            null,
            null,
            maxAgeSeconds,
            cookiesConfig.secure,
            true    // httpOnly
        )
    }

    private fun hash(token: String) = Hashing.sha1().hashString(token, Charsets.UTF_8).toString()

    companion object {
        const val NONCE_COOKIE = "nonce"
    }
}
