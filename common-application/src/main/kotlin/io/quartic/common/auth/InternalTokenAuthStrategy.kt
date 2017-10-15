package io.quartic.common.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.decodeAsBase64
import java.time.Clock
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders.AUTHORIZATION

class InternalTokenAuthStrategy(
    config: TokenAuthConfiguration,
    codec: SecretsCodec,
    clock: Clock = Clock.systemUTC()
) : AuthStrategy<String> {
    private val LOG by logger()

    private val parser = Jwts.parser()
        .setClock({ Date.from(clock.instant()) })
        .setSigningKey(SecretKeySpec(
            codec.decrypt(config.keyEncryptedBase64).veryUnsafe.decodeAsBase64(),
            ALGORITHM.toString()
        ))

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

    // TODO - we're going to have to embed the namespace list into the User principal
    override fun authenticate(creds: String): User? {
        // TODO - do we require any particular claims?

        val claims = try {
            parser.parseClaimsJws(creds)
        } catch (e: Exception) {
            LOG.warn("JWT parsing failed: ${e.message}")    // Logging the whole stack trace is annoying
            return null
        }

        val namespaces = (claims.body[NAMESPACES_CLAIM] as List<String>?)   // TODO - how do we type-convert here?
        if (customerId == null) {
            LOG.warn("Customer ID claim is missing or unparsable")
            return null
        }

        return User(subject, customerId)
    }

    companion object {
        // We can use HMAC for now as client-side verification of tokens is not an issue
        val ALGORITHM = SignatureAlgorithm.HS512
        val KEY_LENGTH_BITS = 512
        val NAMESPACES_CLAIM = "nss"
    }
}
