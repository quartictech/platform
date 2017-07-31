package io.quartic.mgmt

import feign.Contract
import feign.FeignException
import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.getIssuer
import io.quartic.common.client.client
import java.net.URI
import java.net.URLEncoder
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


@Path("/auth")
class AuthResource(private val githubConfig: GithubConfiguration,
                   private val tokenGenerator: TokenGenerator) {
    private val githubOauth = client<GitHubOAuth>(javaClass, githubConfig.oauthApiRoot)
    private val githubApi = client<GitHub>(javaClass, githubConfig.apiRoot)

    @GET
    @Path("/gh")
    fun github(@HeaderParam(HttpHeaders.HOST) host: String): Response? {
        val issuer = getIssuer(host)
        val redirectUri = "${githubConfig.trampolineUrl}/${issuer}"
        val uri = oauthUrl(githubConfig.clientId, redirectUri, githubConfig.scopes)
        return Response.temporaryRedirect(uri).build()
    }

    @GET
    @Path("/gh/callback/{issuer}")
    fun githubCallback(@PathParam("issuer") issuer: String, @QueryParam("code") code: String): Response? {
        val redirectHost = String.format(githubConfig.redirectHost, issuer)
        val uri = URI.create("${redirectHost}/#/login?provider=gh&code=${URLEncoder.encode(code, Charsets.UTF_8.name())}")
        return Response.temporaryRedirect(uri).build()
    }

    @POST
    @Path("/gh/complete")
    fun githubComplete(@QueryParam("code") code: String,
                       @HeaderParam(HttpHeaders.HOST) host: String,
                       @javax.ws.rs.container.Suspended response: javax.ws.rs.container.AsyncResponse) {
        try {
            val accessToken = githubOauth.accessToken(githubConfig.clientId, githubConfig.clientSecret, githubConfig.trampolineUrl, code).accessToken
            val user = githubApi.user(accessToken)
            val organizations = githubApi.organizations(accessToken).map { org -> org.login }

            if (!organizations.intersect(githubConfig.allowedOrganisations).isEmpty()) {
                val tokens = tokenGenerator.generate(user.login, getIssuer(host))
                response.resume(Response.ok()
                    .header(TokenAuthStrategy.XSRF_TOKEN_HEADER, tokens.xsrf)
                    .cookie(NewCookie(
                        TokenAuthStrategy.TOKEN_COOKIE,
                        tokens.jwt,
                        "/",
                        null,
                        null,
                        githubConfig.cookieMaxAge,
                        githubConfig.useSecureCookies, // secure
                        true // httponly
                    ))
                    .build())
            }
        }
        catch (e: FeignException) {
            if (e.status() in 400..499) {
                response.resume(Response.status(401).build())
            }
            else response.resume(e)
        }
    }
}
