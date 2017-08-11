package io.quartic.common.auth

import com.google.common.hash.Hashing
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.decodeAsBase64
import java.time.Clock
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders

class TokenAuthStrategy(
    config: TokenAuthConfiguration,
    codec: SecretsCodec,
    clock: Clock = Clock.systemUTC()
) : AuthStrategy<Tokens> {
    private val LOG by logger()

    private val parser = Jwts.parser()
        .setClock({ Date.from(clock.instant()) })
        .setSigningKey(SecretKeySpec(
            codec.decrypt(config.keyEncryptedBase64).veryUnsafe.decodeAsBase64(),
            ALGORITHM.toString()
        ))

    override val scheme = "Cookie"      // This is a made-up auth scheme purely to avoid WWW-Authenticate: Basic on 401s

    override fun extractCredentials(requestContext: ContainerRequestContext): Tokens? {
        val jwt = requestContext.cookies[TOKEN_COOKIE]?.value
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
        if (host == null) {
            LOG.error("Host header is missing") // Error because this shouldn't be possible!
            return null
        }

        return Tokens(jwt, xsrf, extractSubdomain(host))
    }

    override fun authenticate(creds: Tokens): User? {
        parser.requireIssuer(creds.issuer)
        parser.require(XSRF_TOKEN_HASH_CLAIM, hashToken(creds.xsrf))

        val claims = try {
            parser.parseClaimsJws(creds.jwt)
        } catch (e: Exception) {
            LOG.warn("JWT parsing failed: ${e.message}")    // Logging the whole stack trace is annoying
            return null
        }

        val subject = claims.body.subject?.toLongOrNull()
        if (subject == null) {
            LOG.warn("Subject claim is missing or unparseable")
            return null
        }

        val customerId = (claims.body[CUSTOMER_ID_CLAIM] as String?)?.toLongOrNull()
        if (customerId == null) {
            LOG.warn("Customer ID claim is missing or unparseable")
            return null
        }

        return User(subject, customerId)
    }

    private fun hashToken(token: String) = Hashing.sha1().hashString(token, Charsets.UTF_8).toString()

    companion object {
        // We can use HMAC for now as client-side verification of tokens is not an issue
        val ALGORITHM = SignatureAlgorithm.HS512
        val KEY_LENGTH_BITS = 512
        val TOKEN_COOKIE = "quartic-token"
        val XSRF_TOKEN_HEADER = "X-XSRF-Token"
        val XSRF_TOKEN_HASH_CLAIM = "xth"
        val CUSTOMER_ID_CLAIM = "cid"
    }

    data class Tokens(
        val jwt: String,
        val xsrf: String,
        val issuer: String
    )
}
