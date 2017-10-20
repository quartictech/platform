package io.quartic.home

import com.google.common.hash.Hashing
import com.nhaarman.mockito_kotlin.*
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.frontend.FrontendTokenGenerator
import io.quartic.common.auth.frontend.FrontendTokenGenerator.Tokens
import io.quartic.common.auth.frontend.FrontendUser
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.test.TOKEN_KEY_BASE64
import io.quartic.common.test.assertThrows
import io.quartic.common.test.exceptionalFuture
import io.quartic.github.*
import io.quartic.home.resource.AuthResource
import io.quartic.home.resource.AuthResource.Companion.NONCE_COOKIE
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import org.apache.http.client.utils.URLEncodedUtils
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.concurrent.CompletableFuture.completedFuture
import javax.ws.rs.BadRequestException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.ServerErrorException
import javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE
import javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT

class AuthResourceShould {
    private val tokenGenerator = mock<FrontendTokenGenerator>()
    private val registry = mock<RegistryServiceClient>()
    private val gitHubOAuth = mock<GitHubOAuthClient>()
    private val gitHub = mock<GitHubClient>()
    private val codec = mock<SecretsCodec> {
        on { decrypt(any()) } doReturn TOKEN_KEY_BASE64
    }
    private val resource = AuthResource(
        GithubConfiguration(
            clientId = "foo",
            clientSecretEncrypted = mock(),
            trampolineUrl = URI("noob"),
            scopes = listOf("user"),
            redirectHost = "http://%s.some.where"
        ),
        CookiesConfiguration(
            secure = true,
            maxAgeSeconds = 30
        ),
        codec,
        tokenGenerator,
        registry,
        gitHubOAuth,
        gitHub
    )

    @Before
    fun before() {
        val customer = mock<Customer> {
            on { id } doReturn CustomerId(6666)
            on { githubOrgId } doReturn 5678
        }
        whenever(registry.getCustomerAsync(any(), anyOrNull())).thenReturn(completedFuture(customer))
        whenever(gitHubOAuth.accessTokenAsync(any(), any(), any(), any())).thenReturn(completedFuture(AccessToken("sweet", null, null)))
        whenever(gitHub.userAsync(AuthToken("sweet")))
            .thenReturn(completedFuture(GitHubUser(1234, "arlo", "Arlo Bryer", URI("http://noob"))))
        whenever(gitHub.organizationsAsync(AuthToken("sweet")))
            .thenReturn(completedFuture(listOf(GitHubOrganization(5678, "quartictech"))))
        whenever(tokenGenerator.generate(FrontendUser(1234, 6666), "localhost")).thenReturn(Tokens("jwt", "xsrf"))
    }

    @Test
    fun generate_nonce_hash_in_cookie_that_matches_redirect_uri() {
        val response = resource.github("yeah")

        with(response) {
            assertThat(status, equalTo(TEMPORARY_REDIRECT.statusCode))
            assertThat(cookies, hasKey(NONCE_COOKIE))

            with(cookies[NONCE_COOKIE]!!) {
                assertTrue(isSecure)
                assertTrue(isHttpOnly)
                assertThat(maxAge, equalTo(DEFAULT_MAX_AGE))
                assertThat(value, equalTo(hash(location.queryParams["state"]!!)))
            }
        }
    }

    private val URI.queryParams
        get() = URLEncodedUtils.parse(this, Charsets.UTF_8).associateBy({ it.name }, { it.value })


    @Test
    fun fail_to_trampoline_if_params_missing() {
        assertThrows<BadRequestException> {
            resource.githubCallback("noobs", null, "xyz")
        }

        assertThrows<BadRequestException> {
            resource.githubCallback("noobs", "abc", null)
        }
    }

    @Test
    fun trampoline_to_correct_subdomain_and_with_correctly_encoded_params() {
        // Note the params will need URL-encoding
        val response = resource.githubCallback("noobs", "abc%def", "uvw%xyz")
        with(response) {
            assertThat(status, equalTo(TEMPORARY_REDIRECT.statusCode))
            assertThat(location.toString(), equalTo("http://noobs.some.where/login?provider=gh&code=abc%25def&state=uvw%25xyz"))
        }
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
        whenever(gitHubOAuth.accessTokenAsync(any(), any(), any(), any())).thenReturn(completedFuture(AccessToken(null, "Bad", "Mofo")))

        assertThrows<NotAuthorizedException> {
            resource.githubComplete("xyz", "abc", hash("abc"), "localhost")
        }
    }

    @Test
    fun reject_if_orgs_dont_overlap() {
        whenever(gitHub.organizationsAsync(AuthToken("sweet")))
            .thenReturn(completedFuture(listOf(GitHubOrganization(9876, "quinticsolutions"))))

        assertThrows<NotAuthorizedException> {
            resource.githubComplete("xyz", "abc", hash("abc"), "localhost")
        }
    }

    @Test
    fun fail_if_github_comms_fail() {
        whenever(gitHub.userAsync(AuthToken("sweet"))).thenReturn(exceptionalFuture())

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
