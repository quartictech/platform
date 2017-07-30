package io.quartic.common.auth

import com.google.common.hash.Hashing
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.logging.logger
import java.time.Clock
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders

class TokenAuthStrategy(config: TokenAuthConfiguration, clock: Clock = Clock.systemUTC()) : AuthStrategy<Tokens> {
    private val LOG by logger()

    private val parser = Jwts.parser()
        .setClock({ Date.from(clock.instant()) })
        .setSigningKey(SecretKeySpec(Base64.getDecoder().decode(config.base64EncodedKey), ALGORITHM.toString()))

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
        val claims = try {
            parser.parseClaimsJws(creds.jwt)
        } catch (e: Exception) {
            LOG.warn("JWT parsing failed", e)
            return null
        }
        if (!hashMatches(claims, creds.xsrf)) return null
        if (!issuerMatches(claims, creds.host)) return null
        val subject = extractSubject(claims) ?: return null
        return User(subject)
    }

    private fun extractSubject(claims: Jws<Claims>): String? {
        val subject = claims.body.subject
        if (subject == null) {
            LOG.warn("Subject claim is missing")
            return null
        }
        return subject
    }

    private fun issuerMatches(claims: Jws<Claims>, host: String): Boolean {
        if (claims.body.issuer != host) {
            LOG.warn("Issuer mismatch (claim: '${claims.body.issuer}', host: '$host')")
            return false
        }
        return true
    }

    private fun hashMatches(claims: Jws<Claims>, xsrfToken: String): Boolean {
        // Comparing hash rather than original value to prevent joint XSS-XSRF attack
        val xthClaim = claims.body[XSRF_TOKEN_HASH_CLAIM]
        val xth = Hashing.sha1().hashString(xsrfToken, Charsets.UTF_8).toString()
        if (xthClaim != xth) {
            LOG.warn("XSRF token mismatch (claim: '$xthClaim', token-hash: '$xth')")
            return false
        }
        return true
    }

    companion object {
        val ALGORITHM = SignatureAlgorithm.HS512
        val XSRF_TOKEN_HEADER = "X-XSRF-Token"
        val XSRF_TOKEN_HASH_CLAIM = "xth"
    }

    data class Tokens(
        val jwt: String,
        val xsrf: String,
        val host: String
    )
}
