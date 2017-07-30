package io.quartic.orf

import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenGenerator
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE
import javax.ws.rs.core.Response

@Path("/token")
class OrfResource(private val tokenGenerator: TokenGenerator) {
    @GET
    fun generateToken(
        @QueryParam("userId") userId: String,
        @HeaderParam(HttpHeaders.HOST) host: String
    ): Response {
        val tokens = tokenGenerator.generate(userId, host)
        return Response.ok()
            .header(XSRF_TOKEN_HEADER, tokens.xsrf)
            .cookie(NewCookie(
                TOKEN_COOKIE,
                tokens.jwt,
                null,
                null,
                null,
                DEFAULT_MAX_AGE,
                true,   // httpOnly
                true    // secure
            ))
            .build()
    }
}
