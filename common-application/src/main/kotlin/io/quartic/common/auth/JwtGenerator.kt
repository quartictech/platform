package io.quartic.common.auth

import com.google.common.base.Preconditions.checkArgument
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.TextCodec.BASE64
import io.quartic.common.auth.TokenAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.TokenAuthStrategy.Companion.KEY_LENGTH_BITS
import io.quartic.common.logging.logger
import io.quartic.common.uid.Uid
import io.quartic.common.uid.UidGenerator
import java.time.Clock
import java.time.temporal.TemporalAmount
import java.util.*

class JwtGenerator(
    private val base64EncodedKey: String,
    private val timeToLive: TemporalAmount,
    private val clock: Clock,
    private val jtiGenerator: UidGenerator<JwtId>
) {
    class JwtId(uid: String) : Uid(uid)

    private val LOG by logger()

    init {
        checkArgument(BASE64.decode(base64EncodedKey).size == KEY_LENGTH_BITS / 8,
            "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    fun generate(user: String, issuer: String): String {
        val jti = jtiGenerator.get()
        LOG.info("Generated JWT with jti '$jti' for '$user@$issuer'")
        // Currently no need for aud - only one audience
        return Jwts.builder()
            .signWith(ALGORITHM, base64EncodedKey)
            .setSubject(user)
            .setIssuer(issuer)
            .setExpiration(Date.from(expiration()))
            .setId(jti.toString())
            .compact()
    }

    private fun expiration() = clock.instant() + timeToLive
}

