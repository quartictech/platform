package io.quartic.common.auth

import com.google.common.hash.Hashing
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.logging.logger
import java.time.Clock
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders

class TokenAuthStrategy(private val jwtVerifier: JwtVerifier) : AuthStrategy<Tokens> {
    constructor(config: TokenAuthConfiguration) : this(JwtVerifier(config.base64EncodedKey, Clock.systemUTC()))

    private val LOG by logger()

    override fun extractCredentials(requestContext: ContainerRequestContext): Tokens? {
        val jwt = requestContext.cookies["token"]
        if (jwt == null) {
            LOG.warn("Token cookie is missing")
            return null
        }

        val xsrf = requestContext.getHeaderString(XSRF_TOKEN_HEADER)
        if (xsrf == null) {
            LOG.warn("X-XSRF-Token header is missing")
            return null
        }

        val host = requestContext.getHeaderString(HttpHeaders.HOST)

        return Tokens(jwt.value, xsrf, host)
    }

    override fun authenticate(creds: Tokens): User? {
        // JwtVerifier already logs, so no need to do so on failure here
        val claims = jwtVerifier.verify(creds.jwt) ?: return null

        val subject = claims.body?.subject
        if (subject == null) {
            LOG.warn("Subject claim is missing")
            return null
        }

        // Comparing hash rather than original value to prevent joint XSS-XSRF attack
        val xthClaim = claims.body[XSRF_TOKEN_HASH_CLAIM]
        val xth = Hashing.sha1().hashString(creds.xsrf, Charsets.UTF_8)
        if (xthClaim != xth) {
            LOG.warn("XSRF token mismatch (claim == '$xthClaim', token-hash = '$xth')")
            return null
        }

        if (claims.body.issuer != creds.host) {
            LOG.warn("Issuer mismatch (claim == '${claims.body.issuer}', host = '${creds.host}')")
            return null
        }

        return User(subject)
    }

    companion object {
        val XSRF_TOKEN_HEADER = "X-XSRF-Token"
        val XSRF_TOKEN_HASH_CLAIM = "xth"
    }

    data class Tokens(
        val jwt: String,
        val xsrf: String,
        val host: String
    )
}
