package io.quartic.orf

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.UnsupportedJwtException
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.sequenceGenerator
import io.quartic.orf.model.JwtId
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class OrfResourceShould {
    private val key = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="    // 512-bit key
    private val timeToLive = Duration.ofMinutes(69)
    private var now = Instant.now()
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val jtiGen = sequenceGenerator(::JwtId)

    private val resource = OrfResource(key, timeToLive, clock, jtiGen)

    @Test
    fun produce_valid_tokens() {
        val jws = parse(resource.authenticate("12345").token)

        assertThat(jws.header.getAlgorithm(), equalTo(OrfResource.ALGORITHM.value))
        assertThat(jws.body.subject, equalTo("12345"))
        assertThat(jws.body.expiration, equalTo(Date.from((now + timeToLive).truncatedTo(ChronoUnit.SECONDS))))
    }

    @Test
    fun produce_unique_jtis() {
        val jwsA = parse(resource.authenticate("12345").token)
        val jwsB = parse(resource.authenticate("12345").token)  // Even for the same userID

        assertThat(jwsB.body.id, not(equalTo(jwsA.body.id)))
    }

    @Test
    fun validate_key_length() {
        assertThrows<IllegalArgumentException> {
            OrfResource("tooshort", timeToLive, clock, jtiGen)
        }
    }

    // This is really just to confirm the behaviour of JJWT
    @Test
    fun use_lib_that_rejects_unsigned_jwts() {
        assertThrows<UnsupportedJwtException> {
            parse(Jwts.builder()
                .setSubject("12345")
                .compact())
        }
    }

    private fun parse(token: String) = Jwts.parser().setSigningKey(key).parseClaimsJws(token)
}
