package io.quartic.common.auth

import com.google.common.hash.Hashing
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.jsonwebtoken.Jwts
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.TokenAuthStrategy.Companion.CUSTOMER_ID_CLAIM
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HASH_CLAIM
import io.quartic.common.auth.TokenGenerator.XsrfId
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.test.TOKEN_KEY_BASE64
import io.quartic.common.test.assertThrows
import io.quartic.common.uid.sequenceGenerator
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

class TokenGeneratorShould {
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val codec = mock<SecretsCodec> {
        on { decrypt(any()) } doReturn TOKEN_KEY_BASE64
    }
    private val generator = TokenGenerator(TokenAuthConfiguration(mock()), codec, timeToLive, clock)

    @Test
    fun generate_valid_tokens() {
        val tokens = generator.generate(User("12345", "67890"), "hello")

        val claims = parse(tokens.jwt)
        val xsrfHash = Hashing.sha1().hashString(tokens.xsrf, Charsets.UTF_8).toString()

        assertThat(claims.header.getAlgorithm(), equalTo(ALGORITHM.value))
        assertThat(claims.body.subject, equalTo("12345"))
        assertThat(claims.body.issuer, equalTo("hello"))
        assertThat(claims.body.expiration, equalTo(Date.from((now + timeToLive).truncatedTo(ChronoUnit.SECONDS))))
        assertThat(claims.body[XSRF_TOKEN_HASH_CLAIM] as String, equalTo(xsrfHash))
        assertThat(claims.body[CUSTOMER_ID_CLAIM] as String, equalTo("67890"))
    }

    @Test
    fun generate_big_fat_xsrf_tokens() {
        val tokens = generator.generate(User("12345", "67890"), "hello")

        assertThat(tokens.xsrf.length, equalTo(32))
    }

    @Test
    fun validate_key_length() {
        val codec = mock<SecretsCodec> {
            on { decrypt(any()) } doReturn UnsafeSecret("abcd")
        }

        assertThrows<IllegalArgumentException> {
            TokenGenerator(TokenAuthConfiguration(mock()), codec, timeToLive, clock, sequenceGenerator(::XsrfId))
        }
    }

    private fun parse(token: String) = Jwts.parser().setSigningKey(TOKEN_KEY_BASE64.veryUnsafe).parseClaimsJws(token)
}
