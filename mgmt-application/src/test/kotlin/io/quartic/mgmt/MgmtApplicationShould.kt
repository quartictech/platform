package io.quartic.mgmt

import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.application.TokenAuthConfiguration
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.auth.User
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.quartic.common.serdes.OBJECT_MAPPER
import org.hamcrest.Matchers.not
import org.junit.Assert.*
import org.junit.ClassRule
import org.junit.Test
import java.util.*

class MgmtApplicationShould {
    init {
        stubFor(post(urlPathEqualTo("/login/oauth/access_token"))
            .withQueryParam("client_id", equalTo(CLIENT_ID))
            .withQueryParam("client_secret", equalTo(CLIENT_SECRET))
            .withQueryParam("redirect_uri", equalTo("http://localhost:${RULE.localPort}/api/auth/gh/callback"))
            .withQueryParam("code", equalTo(CODE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"access_token": "${ACCESS_TOKEN}"}""")))

        stubFor(post(urlPathEqualTo("/login/oauth/access_token"))
            .withQueryParam("client_id", equalTo(CLIENT_ID))
            .withQueryParam("client_secret", equalTo(CLIENT_SECRET))
            .withQueryParam("redirect_uri", equalTo("http://localhost:${RULE.localPort}/api/auth/gh/callback"))
            .withQueryParam("code", equalTo(BAD_CODE))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"error": "noob"}""")))

        stubFor(get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(OBJECT_MAPPER.writeValueAsString(io.quartic.mgmt.User("oliver")))))
        stubFor(get(urlPathEqualTo("/user/orgs"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(OBJECT_MAPPER.writeValueAsString(listOf(io.quartic.mgmt.Organization("noobs"))))))
    }

    @Test
    fun generate_response_with_correct_header_and_cookie() {
        val response = target()
            .queryParam("code", CODE)
            .request()
            .post(null)

        assertThat(response.status, Matchers.equalTo(200))
        assertThat(response.cookies, hasKey(TOKEN_COOKIE))
        assertThat(response.headers, hasKey(XSRF_TOKEN_HEADER))

        assertTrue(!response.cookies[TOKEN_COOKIE]!!.isSecure)
        assertTrue(response.cookies[TOKEN_COOKIE]!!.isHttpOnly)

        val authStrategy = TokenAuthStrategy(TokenAuthConfiguration(KEY))
        val tokens = Tokens(
            response.cookies[TOKEN_COOKIE]!!.value,
            response.headers[XSRF_TOKEN_HEADER]!!.last() as String,
            "localhost"
        )

        assertThat(authStrategy.authenticate(tokens), Matchers.equalTo(User("oliver")))
    }

     @Test
    fun reject_bad_code() {
        val response = target()
            .queryParam("code", BAD_CODE)
            .request()
            .post(null)

        assertThat(response.status, Matchers.equalTo(401))
        assertThat(response.cookies, not(hasKey(TOKEN_COOKIE)))
        assertThat(response.headers, not(hasKey(XSRF_TOKEN_HEADER)))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/auth/gh/complete")

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
            config("github.oauthApiRoot", {"http://localhost:${wireMockRule.port()}"}),
            config("github.apiRoot", {"http://localhost:${wireMockRule.port()}"}),
            config("github.clientId", CLIENT_ID),
            config("github.allowedOrganisations", "noobs"),
            config("github.clientSecret", CLIENT_SECRET)
        )
    }
}
