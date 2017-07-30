package io.quartic.mgmt

import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenGenerator
import java.net.URI
import java.net.URLEncoder
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


@Path("/auth")
class AuthResource(val clientId: String,
                   clientSecret: String,
                   val allowedOrgnisations: Set<String>,
                   private val tokenGenerator: TokenGenerator) {

    val github = Github(clientId, clientSecret, REDIRECT_URI)

    @GET
    @Path("/gh")
    fun github(): Response? {
        val scopes = URLEncoder.encode(listOf("user").joinToString(" "))
        val uri = URI.create("https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${REDIRECT_URI}&scope=${scopes}")
        return Response.temporaryRedirect(uri).build()
    }

    @GET
    @Path("/gh/callback")
    fun githubCallback(@QueryParam("code") code: String): Response? {
        val uri = URI.create("http://localhost:3010/#/login?provider=gh&code=${code}")
        return Response.temporaryRedirect(uri).build()
    }

    @POST
    @Path("/gh/complete")
    fun githubDo(@QueryParam("code") code: String,
                 @HeaderParam(HttpHeaders.HOST) host: String): Response {
        val accessToken = github.accessToken(code)
        val user = github.user(accessToken)
        val organizations = github.organizations(accessToken).map { org -> org.login }

        if (!organizations.intersect(allowedOrgnisations).isEmpty()) {
            val tokens = tokenGenerator.generate(user.login, host)
            return Response.ok()
                .header(TokenAuthStrategy.XSRF_TOKEN_HEADER, tokens.xsrf)
                .cookie(NewCookie(
                    TokenAuthStrategy.TOKEN_COOKIE,
                    tokens.jwt,
                    "/",
                    null,
                    null,
                    NewCookie.DEFAULT_MAX_AGE,
                    false, // secure
                    true // httponly
                ))
                .build()
        }

        return Response.status(401).build()
    }

    companion object {
        val COOKIE = "quartic-jwt";
        val REDIRECT_URI = "http://localhost:8100/api/auth/gh/callback"
    }

}
