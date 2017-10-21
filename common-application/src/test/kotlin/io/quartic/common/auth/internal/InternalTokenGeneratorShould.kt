package io.quartic.common.auth.internal

import io.jsonwebtoken.Jwts
import io.quartic.common.auth.frontend.FrontendTokenGenerator
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.NAMESPACES_CLAIM
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.test.TOKEN_KEY_BASE64
import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class InternalTokenGeneratorShould {
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val generator = InternalTokenGenerator(TOKEN_KEY_BASE64, timeToLive, clock)

    @Test
    fun generate_valid_tokens() {
        val token = generator.generate(InternalUser("12345", listOf("abc", "def")))

        val claims = parse(token)

        assertThat(claims.header.getAlgorithm(), equalTo(ALGORITHM.value))
        assertThat(claims.body.subject, equalTo("12345"))
        assertThat(claims.body.expiration, equalTo(Date.from((now + timeToLive).truncatedTo(ChronoUnit.SECONDS))))
        @Suppress("UNCHECKED_CAST")
        assertThat(claims.body[NAMESPACES_CLAIM] as List<String>, contains("abc", "def"))
    }

    @Test
    fun validate_key_length() {
        assertThrows<IllegalArgumentException> {
            FrontendTokenGenerator(UnsafeSecret("abcd"), timeToLive, clock)
        }
    }

    private fun parse(token: String) = Jwts.parser().setSigningKey(TOKEN_KEY_BASE64.veryUnsafe).parseClaimsJws(token)
}
