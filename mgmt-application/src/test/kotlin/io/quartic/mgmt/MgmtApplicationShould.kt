package io.quartic.mgmt

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.junit.WireMockRule
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
import org.apache.http.client.utils.URIBuilder
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.net.URI
import java.util.*
import javax.ws.rs.core.HttpHeaders.LOCATION
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.Family.*

class MgmtApplicationShould {
    init {
        stubFor(get(urlPathEqualTo("/login/oauth/authorize"))
            .withQueryParam("client_id", equalTo(CLIENT_ID))
            .withQueryParam("redirect_uri", matching("http://localhost:${RULE.localPort}/api/auth/gh/callback/.*"))
            .withQueryParam("scope", equalTo("user"))
            .withQueryParam("state", matching(".*"))
            .willReturn(aResponse()
                .withStatus(302)
                .withHeader(LOCATION, "{{request.query.redirect_uri}}?code=$CODE&state={{request.query.state}}")
                .withTransformers("response-template")
            ))

        stubFor(post(urlPathEqualTo("/login/oauth/access_token"))
            .withQueryParam("client_id", equalTo(CLIENT_ID))
            .withQueryParam("client_secret", equalTo(CLIENT_SECRET))
            .withQueryParam("redirect_uri", equalTo("http://localhost:${RULE.localPort}/api/auth/gh/callback"))
            .withQueryParam("code", equalTo(CODE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(mapOf("access_token" to ACCESS_TOKEN)))))

        stubFor(get(urlPathEqualTo("/user"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(io.quartic.mgmt.GitHubUser(1234, "oliver")))))

        stubFor(get(urlPathEqualTo("/user/orgs"))
            .withHeader("Authorization", equalTo("token ${ACCESS_TOKEN}"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(listOf(GitHubOrganization(5678, "noobs"))))))
    }

    private class Browser(var location: URI) {
        private val client = JerseyClientBuilder().build()
            .property(ClientProperties.FOLLOW_REDIRECTS, false)

        private val cookies = mutableMapOf<String, String>()

        fun get() = request { get() }

        fun post() = request { post(null) }

        private fun request(verb: JerseyInvocation.Builder.() -> Response): Response {
            val builder = client.target(location).request()
            cookies.forEach { builder.cookie(it.key, it.value) }
            val response = builder.verb()

            updateCookies(response)
            if (familyOf(response.status) == REDIRECTION) {
                updateLocation(response)
            }
            return response
        }

        private fun updateCookies(response: Response) {
            response.cookies.forEach { cookies[it.key] = it.value.value }
        }

        private fun updateLocation(response: Response) {
            location = response.location
        }
    }

    @Test
    fun support_ete_oauth_flow() {
        val browser = Browser(URI("http://localhost:${RULE.localPort}/api/auth/gh"))

        // Begin process
        browser.get()

        // Query GitHub auth - the stubbing emulates the process which ends in browser being sent to trampoline
        browser.get()

        // Hit trampoline
        browser.get()

        // Extract fragment query params
        val match = ".*code=(.*)&state=(.*)".toRegex().matchEntire(browser.location.fragment)!!
        val code = match.groups[1]!!.value
        val state = match.groups[2]!!.value

        // Finish auth
        browser.location = URIBuilder("http://localhost:${RULE.localPort}/api/auth/gh/complete")
            .setParameter("code", code)
            .setParameter("state", state)
            .build()

        with(browser.post()) {
            assertThat(familyOf(status), equalTo(SUCCESSFUL))

            val authStrategy = TokenAuthStrategy(TokenAuthConfiguration(KEY))
            val tokens = Tokens(
                cookies[TOKEN_COOKIE]!!.value,
                headers[XSRF_TOKEN_HEADER]!!.last() as String,
                "localhost"
            )
            assertThat(authStrategy.authenticate(tokens), equalTo(User(1234, 4321)))
        }
    }

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="
        private val CLIENT_ID = "foo"
        private val CLIENT_SECRET = "bar"
        private val CODE = "good"
        private val ACCESS_TOKEN = UUID.randomUUID().toString()

        @JvmField
        @ClassRule
        var wireMockRule = WireMockRule(wireMockConfig()
            .dynamicPort()
            .extensions(ResponseTemplateTransformer(false))
        )

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
            config("github.clientSecret", CLIENT_SECRET),
            config("github.redirectHost", { "http://localhost:${wireMockRule.port()}" })
        )
    }
}
