package io.quartic.common.auth

import com.google.common.base.Preconditions.checkArgument
import com.google.common.hash.Hashing
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.TextCodec.BASE64
import io.quartic.common.auth.TokenAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.TokenAuthStrategy.Companion.KEY_LENGTH_BITS
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HASH_CLAIM
import io.quartic.common.logging.logger
import io.quartic.common.uid.Uid
import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.common.uid.secureRandomGenerator
import java.time.Clock
import java.time.temporal.TemporalAmount
import java.util.*

class TokenGenerator(
    private val base64EncodedKey: String,
    private val timeToLive: TemporalAmount,
    private val clock: Clock = Clock.systemUTC(),
    private val xsrfTokenGenerator: UidGenerator<XsrfId> = secureRandomGenerator(::XsrfId)
) {
    class XsrfId(uid: String) : Uid(uid)

    private val LOG by logger()

    init {
        checkArgument(BASE64.decode(base64EncodedKey).size == KEY_LENGTH_BITS / 8,
            "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    fun generate(user: String, issuer: String): Tokens {
        LOG.info("Generated JWT for '$user@$issuer'")
        val xsrf = xsrfTokenGenerator.get().uid
        // Currently no need for aud - only one audience, and no need for jti as the custom xth claim suffices as nonce
        return Tokens(
            Jwts.builder()
                .signWith(ALGORITHM, base64EncodedKey)
                .setSubject(user)
                .setIssuer(issuer)
                .setExpiration(Date.from(expiration()))
                .claim(XSRF_TOKEN_HASH_CLAIM, Hashing.sha1().hashString(xsrf, Charsets.UTF_8).toString())
                .compact(),
            xsrf
        )
    }

    private fun expiration() = clock.instant() + timeToLive

    data class Tokens(val jwt: String, val xsrf: String)
}

