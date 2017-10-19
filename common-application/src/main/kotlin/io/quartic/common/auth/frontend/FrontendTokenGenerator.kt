package io.quartic.common.auth.frontend

import com.google.common.base.Preconditions.checkArgument
import com.google.common.hash.Hashing
import io.jsonwebtoken.Jwts
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.CUSTOMER_ID_CLAIM
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.KEY_LENGTH_BITS
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.XSRF_TOKEN_HASH_CLAIM
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.secrets.decodeAsBase64
import io.quartic.common.uid.Uid
import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.secureRandomGenerator
import java.time.Clock
import java.time.temporal.TemporalAmount
import java.util.*

class FrontendTokenGenerator(
    signingKeyBase64: UnsafeSecret,
    private val timeToLive: TemporalAmount,
    private val clock: Clock = Clock.systemUTC(),
    private val xsrfTokenGenerator: UidGenerator<XsrfId> = secureRandomGenerator(::XsrfId)
) {
    class XsrfId(uid: String) : Uid(uid)

    private val key = signingKeyBase64.veryUnsafe.decodeAsBase64()

    init {
        checkArgument(key.size == KEY_LENGTH_BITS / 8, "Key is not exactly $KEY_LENGTH_BITS bits long")
    }

    fun generate(user: FrontendUser, issuer: String): Tokens {
        val xsrf = xsrfTokenGenerator.get().uid
        // Currently no need for aud - only one audience, and no need for jti as token is long-lived / multi-use
        return Tokens(
            Jwts.builder()
                .signWith(ALGORITHM, key)
                .setSubject(user.id)
                .setIssuer(issuer)
                .setExpiration(Date.from(expiration()))
                .claim(CUSTOMER_ID_CLAIM, user.customerId.uid)
                .claim(XSRF_TOKEN_HASH_CLAIM, Hashing.sha1().hashString(xsrf, Charsets.UTF_8).toString())
                .compact(),
            xsrf
        )
    }

    private fun expiration() = clock.instant() + timeToLive

    data class Tokens(val jwt: String, val xsrf: String)
}

