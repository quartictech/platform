package io.quartic.mgmt

import com.google.common.hash.Hashing
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import feign.FeignException
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.TokenGenerator.Tokens
import io.quartic.common.test.assertThrows
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.ServerErrorException

class AuthResourceShould {
    private val tokenGenerator = mock<TokenGenerator>()
    private val gitHubOAuth = mock<GitHubOAuth>()
    private val gitHub = mock<GitHub>()
    private val resource = AuthResource(
        GithubConfiguration(
            clientId = "foo",
            clientSecret = "bar",
            allowedOrganisations = setOf("quartictech"),
            trampolineUrl = "noob",
            scopes = listOf("user"),
            redirectHost = "wat"
        ),
        CookiesConfiguration(
            secure = true,
            maxAgeSeconds = 30
        ),
        tokenGenerator, gitHubOAuth, gitHub
    )

    @Before
    fun before() {
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(AccessToken("sweet", null, null))
        whenever(gitHub.user("sweet")).thenReturn(User("arlo"))
        whenever(gitHub.organizations("sweet")).thenReturn(listOf(Organization("quartictech")))
        whenever(tokenGenerator.generate("arlo", "localhost")).thenReturn(Tokens("jwt", "xsrf"))
    }

    @Test
    fun reject_if_params_missing() {
        assertThrows<BadRequestException> {
            resource.githubComplete(null, "abc", hash("abc"), "localhost")
        }

        assertThrows<BadRequestException> {
            resource.githubComplete("xyz", null, hash("abc"), "localhost")
        }

        assertThrows<BadRequestException> {
            resource.githubComplete("xyz", "abc", null, "localhost")
        }
    }

    @Test
    fun reject_if_nonce_mismatch() {
        assertThrows<NotAuthorizedException> {
            resource.githubComplete("xyz", "abc", hash("def"), "localhost")
        }
    }

    @Test
    fun reject_if_github_rejects() {
        whenever(gitHubOAuth.accessToken(any(), any(), any(), any())).thenReturn(AccessToken(null, "Bad", "Mofo"))

        assertThrows<NotAuthorizedException> {
            resource.githubComplete("xyz", "abc", hash("abc"), "localhost")
        }
    }

    @Test
    fun reject_if_orgs_dont_overlap() {
        whenever(gitHub.organizations("sweet")).thenReturn(listOf(Organization("quinticsolutions")))

        assertThrows<NotAuthorizedException> {
            resource.githubComplete("xyz", "abc", hash("abc"), "localhost")
        }
    }

    @Test
    fun fail_if_github_comms_fail() {
        whenever(gitHub.user("sweet")).thenThrow(mock<FeignException>())

        assertThrows<ServerErrorException> {
            resource.githubComplete("xyz", "abc", hash("abc"), "localhost")
        }
    }

    @Test
    fun generate_tokens_if_orgs_whitelisted() {
        val response = resource.githubComplete("xyz", "abc", hash("abc"), "localhost")

        with(response) {
            assertThat(status, equalTo(200))
            with (cookies[TOKEN_COOKIE]!!) {
                assertThat(value, equalTo("jwt"))
                assertTrue(isHttpOnly)
                assertTrue(isSecure)
            }
            assertThat(headers[XSRF_TOKEN_HEADER]!!.last() as String, equalTo("xsrf"))
        }
    }

    private fun hash(token: String) = Hashing.sha1().hashString(token, Charsets.UTF_8).toString()
}
