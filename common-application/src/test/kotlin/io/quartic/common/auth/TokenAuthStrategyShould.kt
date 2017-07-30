package io.quartic.common.auth

import com.google.common.hash.Hashing
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HASH_CLAIM
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.HttpHeaders

class TokenAuthStrategyShould {
    val requestContext = mock<ContainerRequestContext> {
        on { cookies } doReturn mapOf("token" to Cookie("token", "abc"))
        on { getHeaderString(XSRF_TOKEN_HEADER) } doReturn "def"
        on { getHeaderString(HttpHeaders.HOST) } doReturn "noob.quartic.io"
    }
    private val claims = mock<Jws<Claims>>()
    private val jwtVerifier = mock<JwtVerifier> {
        on { verify(anyOrNull()) } doReturn claims
    }
    private val strategy = TokenAuthStrategy(jwtVerifier)
    private val tokens = Tokens("abc", "def", "noob.quartic.io")

    @Before
    fun before() {
        // Correct behaviour
        val claimsBody = mock<Claims> {
            on { subject } doReturn "oliver"
            on { issuer } doReturn "noob.quartic.io"
            on { get(XSRF_TOKEN_HASH_CLAIM) } doReturn Hashing.sha1().hashString("def", Charsets.UTF_8)
        }
        whenever(claims.body).thenReturn(claimsBody)
    }

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
        assertThat(strategy.authenticate(tokens), equalTo(User("oliver")))
    }

    @Test
    fun reject_when_jwt_verification_fails() {
        whenever(jwtVerifier.verify(anyOrNull())).thenReturn(null)

        assertAuthenticationFails()
    }

    @Test
    fun reject_when_subject_claim_missing() {
        whenever(claims.body.subject).thenReturn(null)

        assertAuthenticationFails()
    }

    @Test
    fun reject_when_xth_claim_missing() {
        whenever(claims.body[XSRF_TOKEN_HASH_CLAIM]).thenReturn(null)

        assertAuthenticationFails()
    }

    @Test
    fun reject_when_xsrf_token_mismatch() {
        whenever(claims.body[XSRF_TOKEN_HASH_CLAIM]).thenReturn("wrong-hash")

        assertAuthenticationFails()
    }

    @Test
    fun reject_when_iss_claim_missing() {
        whenever(claims.body.issuer).thenReturn(null)

        assertAuthenticationFails()
    }

    @Test
    fun reject_when_host_mismatch() {
        whenever(claims.body.issuer).thenReturn("wrong.quartic.io")

        assertAuthenticationFails()
    }

    private fun assertAuthenticationFails() {
        assertThat(strategy.authenticate(tokens), nullValue())
    }
}
