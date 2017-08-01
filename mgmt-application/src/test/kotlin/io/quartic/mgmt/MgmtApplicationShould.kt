package io.quartic.mgmt

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.common.hash.Hashing
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.auth.User
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.mgmt.AuthResource.Companion.NONCE_COOKIE
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicNameValuePair
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response.Status.UNAUTHORIZED

class MgmtApplicationShould {
    init {
        stubFor(postForAccessToken()
            .withQueryParam("code", equalTo(CODE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(mapOf("access_token" to ACCESS_TOKEN)))))

        stubFor(postForAccessToken()
            .withQueryParam("code", equalTo(BAD_CODE))
            .willReturn(aResponse()
                .withStatus(200)    // Yes, GitHub returns 200 on error
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(mapOf("error" to "noob")))))

        stubFor(get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(io.quartic.mgmt.User("oliver")))))

        stubFor(get(urlPathEqualTo("/user/orgs"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(listOf(Organization("noobs"))))))
    }

    private fun postForAccessToken() = post(urlPathEqualTo("/login/oauth/access_token"))
        .withQueryParam("client_id", equalTo(CLIENT_ID))
        .withQueryParam("client_secret", equalTo(CLIENT_SECRET))
        .withQueryParam("redirect_uri", equalTo("http://localhost:${RULE.localPort}/api/auth/gh/callback"))

    @Test
    fun generate_tokens_via_correct_header_and_cookie() {
        val response = target("/complete",
            mapOf(
                "code" to CODE,
                "state" to "abcdefg"
            ))
            .request()
            .cookie(NONCE_COOKIE, hash("abcdefg"))
            .post(null)

        with(response) {
            assertThat(status, equalTo(200))
            assertThat(cookies, hasKey(TOKEN_COOKIE))
            assertThat(headers, hasKey(XSRF_TOKEN_HEADER))

            with(cookies[TOKEN_COOKIE]!!) {
                assertTrue(isSecure)
                assertTrue(isHttpOnly)
            }

            val authStrategy = TokenAuthStrategy(TokenAuthConfiguration(KEY))
            val tokens = Tokens(
                cookies[TOKEN_COOKIE]!!.value,
                headers[XSRF_TOKEN_HEADER]!!.last() as String,
                "localhost"
            )
            assertThat(authStrategy.authenticate(tokens), equalTo(User("oliver")))
        }
    }

    @Test
    fun reject_bad_code() {
        val response = target("/complete",
            mapOf(
                "code" to BAD_CODE,
                "state" to "abcdefg"
            ))
            .request()
            .cookie(NONCE_COOKIE, hash("abcdefg"))
            .post(null)

        with(response) {
            assertThat(status, equalTo(UNAUTHORIZED.statusCode))
            assertThat(cookies, not(hasKey(TOKEN_COOKIE)))
            assertThat(headers, not(hasKey(XSRF_TOKEN_HEADER)))
        }
    }

    // JerseyWebTarget uses UriBuilder under the hood, which is noob.  So we have to do this instead for query params.
    private fun target(suffix: String, queryParams: Map<String, String> = emptyMap()) = JerseyClientBuilder().build()
        .target(
            URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPort(RULE.localPort)
                .setPath("/api/auth/gh${suffix}")
                .setParameters(queryParams.map { BasicNameValuePair(it.key, it.value) })
                .build()
        )
        .property(ClientProperties.FOLLOW_REDIRECTS, false)

    private fun hash(token: String) = Hashing.sha1().hashString(token, Charsets.UTF_8).toString()

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="
        private val CLIENT_ID = "foo"
        private val CLIENT_SECRET = "bar"
        private val CODE = "good"
        private val BAD_CODE = "bad"
        private val ACCESS_TOKEN = UUID.randomUUID().toString()

        @JvmField
        @ClassRule
        var wireMockRule = WireMockRule(DYNAMIC_PORT)

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<MgmtConfiguration>(
            MgmtApplication::class.java,
            resourceFilePath("test.yml"),
            config("auth.type", "token"),
            config("auth.base64EncodedKey", KEY),
            config("github.oauthApiRoot", { "http://localhost:${wireMockRule.port()}" }),
            config("github.apiRoot", { "http://localhost:${wireMockRule.port()}" }),
            config("github.clientId", CLIENT_ID),
            config("github.allowedOrganisations", "noobs"),
            config("github.clientSecret", CLIENT_SECRET)
        )
    }
}
