package io.quartic.common.auth

import io.jsonwebtoken.Jwts
import io.quartic.common.auth.JwtGenerator.JwtId
import io.quartic.common.auth.TokenAuthStrategy.Companion.ALGORITHM
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.sequenceGenerator
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class JwtGeneratorShould {
    private val key = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="    // 512-bit key
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val generator = JwtGenerator(key, timeToLive, clock, sequenceGenerator(::JwtId))

    @Test
    fun generate_valid_token() {
        val jws = parse(generator.generate("12345", "hello"))

        assertThat(jws.header.getAlgorithm(), equalTo(ALGORITHM.value))
        assertThat(jws.body.subject, equalTo("12345"))
        assertThat(jws.body.issuer, equalTo("hello"))
        assertThat(jws.body.expiration, equalTo(Date.from((now + timeToLive).truncatedTo(ChronoUnit.SECONDS))))
    }

    @Test
    fun generate_unique_jtis() {
        val jwsA = parse(generator.generate("12345", "abc"))
        val jwsB = parse(generator.generate("12345", "def"))  // Even for the same userID

        assertThat(jwsB.body.id, Matchers.not(equalTo(jwsA.body.id)))
    }

    @Test
    fun validate_key_length() {
        assertThrows<IllegalArgumentException> {
            JwtGenerator("tooshort", timeToLive, clock, sequenceGenerator(::JwtId))
        }
    }

    private fun parse(token: String) = Jwts.parser().setSigningKey(key).parseClaimsJws(token)
}
