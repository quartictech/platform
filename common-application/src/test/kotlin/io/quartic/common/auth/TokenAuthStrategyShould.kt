package io.quartic.common.auth

import com.google.common.hash.Hashing
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy.Companion.ALGORITHM
import io.quartic.common.auth.TokenAuthStrategy.Companion.CUSTOMER_ID_CLAIM
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HASH_CLAIM
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenAuthStrategy.Tokens
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
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.HttpHeaders

class TokenAuthStrategyShould {
    private val now = Instant.now()
    private val timeToLive = Duration.ofMinutes(69)
    private val past = now - timeToLive
    private val future = now + timeToLive
    private val clock = Clock.fixed(now, ZoneId.systemDefault())
    private val codec = mock<SecretsCodec> {
        on { decrypt(any()) } doReturn TOKEN_KEY_BASE64
    }

    private val requestContext = mock<ContainerRequestContext> {
        on { cookies } doReturn mapOf(TOKEN_COOKIE to Cookie(TOKEN_COOKIE, "abc"))
        on { getHeaderString(XSRF_TOKEN_HEADER) } doReturn "def"
        on { getHeaderString(HttpHeaders.HOST) } doReturn "noob.quartic.io"
    }
    private val strategy = TokenAuthStrategy(TokenAuthConfiguration(mock()), codec, clock)
    private val tokens = Tokens("abc", "def", "noob")

    @Test
    fun extract_tokens_when_present() {
        assertThat(strategy.extractCredentials(requestContext), equalTo(tokens))
    }

    @Test
    fun fail_to_extract_when_token_cookie_missing() {
        whenever(requestContext.cookies) doReturn emptyMap()

        assertThat(strategy.extractCredentials(requestContext), nullValue())
    }

    @Test
    fun fail_to_extract_when_xsrf_header_missing() {
        whenever(requestContext.getHeaderString(XSRF_TOKEN_HEADER)).thenReturn(null)

        assertThat(strategy.extractCredentials(requestContext), nullValue())
    }

    @Test
    fun accept_valid_tokens() {
        val tokens = tokens { this }

        assertThat(strategy.authenticate(tokens), equalTo(User(1234, 5678)))
    }

    @Test
    fun reject_token_with_invalid_signature() {
        assertAuthenticationFails(tokens {
            signWith(ALGORITHM, "CffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q==")    // Wrong key!
        })
    }

    @Test
    fun reject_unsigned_token() {
        assertAuthenticationFails(tokens {
            // This is gross, but JwtBuilder has no way to remove signature setting
            val field = DefaultJwtBuilder::class.java.getDeclaredField("keyBytes")
            field.isAccessible = true
            field.set(this, null)
            this
        })
    }

    @Test
    fun reject_expired_token() {
        assertAuthenticationFails(tokens {
            setExpiration(Date.from(past))
        })
    }

    @Test
    fun reject_unparseable_token() {
        assertAuthenticationFails(Tokens("def", "noob.quartic.io", "gibberish"))
    }

    @Test
    fun reject_when_subject_claim_missing() {
        assertAuthenticationFails(tokens {
            setSubject(null)
        })
    }

    @Test
    fun reject_when_subject_claim_not_an_int() {
        assertAuthenticationFails(tokens {
            setSubject("not_an_int")
        })
    }

    @Test
    fun reject_when_cid_claim_missing() {
        assertAuthenticationFails(tokens {
            claim(CUSTOMER_ID_CLAIM, null)
        })
    }

    @Test
    fun reject_when_cid_claim_not_an_int() {
        assertAuthenticationFails(tokens {
            claim(CUSTOMER_ID_CLAIM, "not_an_int")
        })
    }

    @Test
    fun reject_when_xth_claim_missing() {
        assertAuthenticationFails(tokens {
            claim(XSRF_TOKEN_HASH_CLAIM, null)
        })
    }

    @Test
    fun reject_when_xsrf_token_mismatch() {
        assertAuthenticationFails(tokens {
            claim(XSRF_TOKEN_HASH_CLAIM, "wrong-hash")
        })
    }

    @Test
    fun reject_when_iss_claim_missing() {
        assertAuthenticationFails(tokens {
            setIssuer(null)
        })
    }

    @Test
    fun reject_when_host_mismatch() {
        assertAuthenticationFails(tokens {
            setIssuer("wrong.quartic.io")
        })
    }

    private fun assertAuthenticationFails(tokens: Tokens) {
        assertThat(strategy.authenticate(tokens), nullValue())
    }

    private fun tokens(builderMods: JwtBuilder.() -> JwtBuilder) = Tokens(
        Jwts.builder()
            .signWith(ALGORITHM, TOKEN_KEY_BASE64)
            .setSubject("1234")
            .setIssuer("noob")
            .setExpiration(Date.from(future))
            .claim(XSRF_TOKEN_HASH_CLAIM, Hashing.sha1().hashString("def", Charsets.UTF_8).toString())
            .claim(CUSTOMER_ID_CLAIM, "5678")
            .builderMods()
            .compact(),
        "def",
        "noob"
    )
}
