package io.quartic.common.auth.internal

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.auth.AuthStrategy
import io.quartic.common.logging.logger
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.secrets.decodeAsBase64
import java.time.Clock
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders.AUTHORIZATION

class InternalAuthStrategy(
    signingKeyBase64: UnsafeSecret,
    clock: Clock = Clock.systemUTC()
) : AuthStrategy<String, InternalUser> {
    private val LOG by logger()

    private val parser = Jwts.parser()
        .setClock({ Date.from(clock.instant()) })
        .setSigningKey(SecretKeySpec(signingKeyBase64.veryUnsafe.decodeAsBase64(), ALGORITHM.toString()))

    override val principalClass = InternalUser::class.java
    override val scheme = "Cookie"      // TODO - what makes sense for internal stuff?


    // TODO - maybe consider checking Host header / sender IP

    override fun extractCredentials(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(AUTHORIZATION)
        if (authHeader == null) {
            LOG.warn("Authorization header is missing")
            return null
        }

        val bits = authHeader.trim().split(" ")
        if (bits.size != 2 || bits[0] != "Bearer") {
            LOG.warn("Authorization header is malformed")
            return null
        }

        return bits[1]
    }

    override fun authenticate(creds: String): InternalUser? {
        val claims = try {
            parser.parseClaimsJws(creds)
        } catch (e: Exception) {
            LOG.warn("JWT parsing failed: ${e.message}")    // Logging the whole stack trace is annoying
            return null
        }

        val subject = claims.body.subject
        if (subject == null) {
            LOG.warn("Subject claim is missing or unparsable")
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val namespaces = (claims.body[NAMESPACES_CLAIM] as? List<String>)
        if (namespaces == null) {
            LOG.warn("Namespaces claim is missing or unparsable")
            return null
        }

        return InternalUser(subject, namespaces)
    }

    companion object {
        // We can use HMAC for now as client-side verification of tokens is not an issue
        val ALGORITHM = SignatureAlgorithm.HS512
        val KEY_LENGTH_BITS = 512
        val NAMESPACES_CLAIM = "nss"
    }
}
