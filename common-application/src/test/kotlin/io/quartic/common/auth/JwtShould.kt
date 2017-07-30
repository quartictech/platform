package io.quartic.common.auth

import io.jsonwebtoken.Jwts
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.sequenceGenerator
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/** Note that most of these tests are really validating that jjwt does everything we want. */
class JwtShould {
    private val key = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="    // 512-bit key
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val past = now - timeToLive
    private val future = now + timeToLive
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val generator = JwtGenerator(key, timeToLive, clock, sequenceGenerator(::JwtId))
    private val verifier = JwtVerifier(key, clock)

    @Test
    fun generate_valid_token() {
        val jws = parse(generator.generate("12345", "TODO"))

        assertThat(jws.header.getAlgorithm(), equalTo(ALGORITHM.value))
        assertThat(jws.body.subject, equalTo("12345"))
        assertThat(jws.body.expiration, equalTo(Date.from((now + timeToLive).truncatedTo(ChronoUnit.SECONDS))))
    }

    @Test
    fun generate_unique_jtis() {
        val jwsA = parse(generator.generate("12345", "TODO"))
        val jwsB = parse(generator.generate("12345", "TODO"))  // Even for the same userID

        assertThat(jwsB.body.id, Matchers.not(equalTo(jwsA.body.id)))
    }

    @Test
    fun validate_key_length() {
        assertThrows<IllegalArgumentException> {
            JwtGenerator("tooshort", timeToLive, clock, sequenceGenerator(::JwtId))
        }
    }

    @Test
    fun accept_token_with_valid_signature() {
        val token = Jwts.builder()
            .signWith(ALGORITHM, key)
            .setSubject("abc")
            .setExpiration(Date.from(future))
            .setId("xyz")
            .compact()

        assertThat(verifier.verify(token)!!.body.subject, equalTo("abc"))
    }

    @Test
    fun reject_token_with_invalid_signature() {
        assertRejectedToken(Jwts.builder()
            .signWith(ALGORITHM, "CffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q==")    // Different!
            .setSubject("abc")
            .setExpiration(Date.from(future))
            .setId("xyz")
            .compact())
    }

    @Test
    fun reject_unsigned_token() {
        assertRejectedToken(Jwts.builder()
            .setSubject("abc")
            .setExpiration(Date.from(future))
            .setId("xyz")
            .compact())
    }

    @Test
    fun reject_expired_token() {
        assertRejectedToken(Jwts.builder()
            .signWith(ALGORITHM, key)
            .setSubject("abc")
            .setExpiration(Date.from(past))
            .setId("xyz")
            .compact())
    }

    @Test
    fun reject_unparseable_token() {
        assertRejectedToken("nonsense")
    }

    private fun assertRejectedToken(token: String) {
        assertThat(verifier.verify(token), nullValue())
    }

    private fun parse(token: String) = Jwts.parser().setSigningKey(key).parseClaimsJws(token)
}
