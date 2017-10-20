package io.quartic.common.auth.internal

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.quartic.common.application.InternalAuthConfiguration
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.internal.InternalAuthStrategy.Companion.NAMESPACES_CLAIM
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.test.TOKEN_KEY_BASE64
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.HttpHeaders

class InternalAuthStrategyShould {
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val past = now - timeToLive
    private val future = now + timeToLive
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val codec = mock<SecretsCodec> {
        on { decrypt(any()) } doReturn TOKEN_KEY_BASE64
    }

    private val requestContext = mock<ContainerRequestContext> {
        on { getHeaderString(HttpHeaders.AUTHORIZATION) } doReturn "Bearer 912746912764"
    }
    private val strategy = InternalAuthStrategy(InternalAuthConfiguration(mock()), codec, clock)

    @Test
    fun extract_token_when_present() {
        assertThat(strategy.extractCredentials(requestContext), equalTo("912746912764"))
    }

    @Test
    fun fail_to_extract_when_auth_header_missing() {
        whenever(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null)

        assertThat(strategy.extractCredentials(requestContext), nullValue())
    }

    @Test
    fun fail_to_extract_when_auth_header_malformed() {
        whenever(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("NoobholeXE23")

        assertThat(strategy.extractCredentials(requestContext), nullValue())
    }

    @Test
    fun accept_valid_tokens() {
        val token = token { this }

        assertThat(strategy.authenticate(token), equalTo(InternalUser("1234", listOf("abc", "def"))))
    }

    @Test
    fun reject_token_with_invalid_signature() {
        assertAuthenticationFails(token {
            signWith(ALGORITHM, "CffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q==")    // Wrong key!
        })
    }

    @Test
    fun reject_unsigned_token() {
        assertAuthenticationFails(token {
            // This is gross, but JwtBuilder has no way to remove signature setting
            val field = DefaultJwtBuilder::class.java.getDeclaredField("keyBytes")
            field.isAccessible = true
            field.set(this, null)
            this
        })
    }

    @Test
    fun reject_expired_token() {
        assertAuthenticationFails(token {
            setExpiration(Date.from(past))
        })
    }

    @Test
    fun reject_unparsable_token() {
        assertAuthenticationFails("gibberish")
    }

    @Test
    fun reject_when_subject_claim_missing() {
        assertAuthenticationFails(token {
            setSubject(null)
        })
    }

    @Test
    fun reject_when_nss_claim_missing() {
        assertAuthenticationFails(token {
            claim(NAMESPACES_CLAIM, null)
        })
    }

    @Test
    fun reject_when_nss_claim_not_a_list_of_strings() {
        assertAuthenticationFails(token {
            claim(NAMESPACES_CLAIM, "not_a_list_of_strings")
        })
    }

    private fun assertAuthenticationFails(token: String) {
        assertThat(strategy.authenticate(token), nullValue())
    }

    private fun token(builderMods: JwtBuilder.() -> JwtBuilder) = Jwts.builder()
        .signWith(ALGORITHM, TOKEN_KEY_BASE64.veryUnsafe)
        .setSubject("1234")
        .setExpiration(Date.from(future))
        .claim(NAMESPACES_CLAIM, listOf("abc", "def"))
        .builderMods()
        .compact()
}
