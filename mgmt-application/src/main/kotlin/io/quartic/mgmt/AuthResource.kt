package io.quartic.mgmt

import java.net.URI
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


@Path("/auth")
class AuthResource {
    @GET
    @Path("/gh")
    fun github(): Response? {
        val clientId = "af4a5c01c7850fa04758"
        val redirectUri = "http://localhost:8100/api/auth/gh/callback"
        val uri = URI.create("https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}")
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
    fun githubDo(@QueryParam("code") code: String): Response {
        val cookie = NewCookie("quartic-jwt", "1", null, null, NewCookie.DEFAULT_VERSION, null, NewCookie.DEFAULT_MAX_AGE, null, false, true)
        return Response.ok()
            .header("Set-Cookie", cookie.toString())
            .header("XSS-Token", 2).build()
    }

}
