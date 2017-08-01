package io.quartic.mgmt

import feign.FeignException
import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.getIssuer
import io.quartic.common.client.client
import io.quartic.common.logging.logger
import java.net.URI
import java.net.URLEncoder
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


@Path("/auth")
class AuthResource(private val gitHubConfig: GithubConfiguration,
                   private val tokenGenerator: TokenGenerator,
                   private val gitHubOAuth: GitHubOAuth = client<GitHubOAuth>(AuthResource::class.java, gitHubConfig.oauthApiRoot),
                   private val gitHubApi: GitHub = client<GitHub>(AuthResource::class.java, gitHubConfig.apiRoot)) {

    val LOG by logger()

    @GET
    @Path("/gh")
    fun github(@HeaderParam(HttpHeaders.HOST) host: String): Response? {
        val issuer = getIssuer(host)
        val redirectUri = "${gitHubConfig.trampolineUrl}/${issuer}"
        val uri = oauthUrl(gitHubConfig.oauthApiRoot, gitHubConfig.clientId, redirectUri, gitHubConfig.scopes)
        return Response.temporaryRedirect(uri).build()
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
                       @javax.ws.rs.container.Suspended response: javax.ws.rs.container.AsyncResponse) {
        try {
            val accessToken = gitHubOAuth.accessToken(gitHubConfig.clientId, gitHubConfig.clientSecret, gitHubConfig.trampolineUrl, code)

            if (accessToken.accessToken == null) {
                LOG.error("Exception while oauthing: {} - {}", accessToken.error, accessToken.errorDescription)
                response.resume(Response.status(401).build())
                return
            } else {
                val user = gitHubApi.user(accessToken.accessToken)
                val organizations = gitHubApi.organizations(accessToken.accessToken).map { org -> org.login }

                if (!organizations.intersect(gitHubConfig.allowedOrganisations).isEmpty()) {
                    val tokens = tokenGenerator.generate(user.login, getIssuer(host))
                    response.resume(Response.ok()
                        .header(TokenAuthStrategy.XSRF_TOKEN_HEADER, tokens.xsrf)
                        .cookie(NewCookie(
                            TokenAuthStrategy.TOKEN_COOKIE,
                            tokens.jwt,
                            "/",
                            null,
                            null,
                            gitHubConfig.cookieMaxAgeSeconds,
                            gitHubConfig.useSecureCookies, // secure
                            true // httponly
                        ))
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
                response.resume(Response.status(401).build())
            }
            else {
                response.resume(Response.status(500).build())
            }
        }
        catch (e: Exception) {
            LOG.error("Exception while authenticating", e)
            response.resume(Response.status(500).build())
        }
    }
}
