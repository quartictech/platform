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
import io.quartic.common.uid.randomGenerator
import io.quartic.common.uid.secureRandomGenerator
import java.net.URI
import java.net.URLEncoder
import javax.ws.rs.*
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE
import javax.ws.rs.core.Response


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
    fun githubCallback(@PathParam("issuer") issuer: String, @QueryParam("code") code: String): Response? {
        val redirectHost = String.format(gitHubConfig.redirectHost, issuer)
        val uri = URI.create("${redirectHost}/#/login?provider=gh&code=${URLEncoder.encode(code, Charsets.UTF_8.name())}")
        return Response.temporaryRedirect(uri).build()
    }

    @POST
    @Path("/gh/complete")
    fun githubComplete(@QueryParam("code") code: String,
                       @HeaderParam(HttpHeaders.HOST) host: String,
                       @Suspended response: AsyncResponse) {
        try {
            val accessToken = gitHubOAuth.accessToken(
                gitHubConfig.clientId,
                gitHubConfig.clientSecret,
                gitHubConfig.trampolineUrl,
                code
            ).accessToken
            val user = gitHubApi.user(accessToken)
            val organizations = gitHubApi.organizations(accessToken).map { org -> org.login }

            if (!organizations.intersect(gitHubConfig.allowedOrganisations).isEmpty()) {
                val tokens = tokenGenerator.generate(user.login, getIssuer(host))
                response.resume(Response.ok()
                    .header(XSRF_TOKEN_HEADER, tokens.xsrf)
                    .cookie(cookie(TOKEN_COOKIE, tokens.jwt, cookiesConfig.maxAgeSeconds))
                    .build())
            }
            else {
                response.resume(Response.status(401).build())
            }
        }
        catch (e: FeignException) {
            if (e.status() in 400..499) {
                response.resume(Response.status(401).build())
            }
            else {
                LOG.error("Exception communicating with GitHub", e)
                response.resume(Response.status(500).build())
            }
        }
    }

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
        val NONCE_COOKIE = "nonce"
    }
}
