package io.quartic.common.auth.internal

import com.google.common.base.Preconditions.checkArgument
import io.jsonwebtoken.Jwts
import io.quartic.common.application.InternalAuthConfiguration
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.KEY_LENGTH_BITS
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.NAMESPACES_CLAIM
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.decodeAsBase64
import java.time.Clock
import java.time.temporal.TemporalAmount
import java.util.*

class InternalTokenGenerator(
    config: InternalAuthConfiguration,
    codec: SecretsCodec,
    private val timeToLive: TemporalAmount,
    private val clock: Clock = Clock.systemUTC()
) {
    private val key = codec.decrypt(config.keyEncryptedBase64).veryUnsafe.decodeAsBase64()

    init {
        checkArgument(key.size == KEY_LENGTH_BITS / 8, "Key is not exactly ${KEY_LENGTH_BITS} bits long")
    }

    fun generate(user: InternalUser): String {
        // Currently doesn't make sense to have jti - we anticipate that the client may re-use the token before it expires
        // Though if we decide to re-architect things for one JWT per dataset read/write, then things may change.

        return Jwts.builder()
            .signWith(ALGORITHM, key)
            .setSubject(user.id)
            .setExpiration(Date.from(expiration()))
            .claim(NAMESPACES_CLAIM, user.namespaces)
            .compact()
    }

    private fun expiration() = clock.instant() + timeToLive
}
