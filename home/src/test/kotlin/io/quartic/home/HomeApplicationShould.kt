package io.quartic.home

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.application.DEV_MASTER_KEY_BASE64
import io.quartic.common.auth.frontend.FrontendAuthStrategy
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.TOKEN_KEY_BASE64
import io.quartic.github.GitHubOrganization
import io.quartic.github.GitHubUser
import io.quartic.registry.api.model.Customer
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
import javax.ws.rs.core.HttpHeaders.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.Family.*

class HomeApplicationShould {
    init {
        with(github) {
            stubFor(get(urlPathEqualTo("/login/oauth/authorize"))
                .withQueryParam("client_id", equalTo(CLIENT_ID))
                .withQueryParam("redirect_uri", matching("http://localhost:${trampolineProxy.port()}/api/auth/gh/callback/.*"))
                .withQueryParam("scope", equalTo("user"))
                .withQueryParam("state", matching(".*"))
                .willReturn(aResponse()
                    .withStatus(302)
                    .withHeader(LOCATION, "{{request.query.redirect_uri}}?code=$CODE&state={{request.query.state}}")
                    .withTransformers("response-template")
                ))

            stubFor(post(urlPathEqualTo("/login/oauth/access_token"))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .withQueryParam("client_id", equalTo(CLIENT_ID))
                .withQueryParam("client_secret", equalTo(CLIENT_SECRET.veryUnsafe))
                .withQueryParam("redirect_uri", equalTo("http://localhost:${trampolineProxy.port()}/api/auth/gh/callback"))
                .withQueryParam("code", equalTo(CODE))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(OBJECT_MAPPER.writeValueAsString(mapOf("access_token" to ACCESS_TOKEN)))))

            stubFor(get(urlPathEqualTo("/user"))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .withHeader(AUTHORIZATION, equalTo("token ${ACCESS_TOKEN}"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(OBJECT_MAPPER.writeValueAsString(GitHubUser(1234, "oliver", "Oliver", URI("http://noob"))))))

            stubFor(get(urlPathEqualTo("/user/orgs"))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .withHeader(AUTHORIZATION, equalTo("token ${ACCESS_TOKEN}"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(OBJECT_MAPPER.writeValueAsString(listOf(GitHubOrganization(5678, "noobs"))))))

            stubFor(get(urlPathEqualTo("/user/1234"))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(OBJECT_MAPPER.writeValueAsString(GitHubUser(1234, "oliver", "Oliver", URI("http://noob"))))))
        }

        with(registry) {
            stubFor(get(urlPathEqualTo("/api/customers"))
                .withQueryParam("subdomain", equalTo("localhost"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(OBJECT_MAPPER.writeValueAsString(Customer(
                        id = CUSTOMER_ID,
                        githubOrgId = 5678,
                        githubRepoId = 8765,
                        githubInstallationId = 123,
                        name = "localhost",
                        subdomain = "localhost",
                        namespace = "localhost"
                    )))
                )
            )
        }

        with(eval) {
            stubFor(get(urlPathEqualTo("/api/query/build/${CUSTOMER_ID}"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(OBJECT_MAPPER.writeValueAsString(emptyList<Any>()))
                )
            )
        }

        with(trampolineProxy) {
            stubFor(get(urlMatching(".*"))
                .willReturn(aResponse().proxiedFrom("http://localhost:${APP.localPort}")))
        }
    }

    private class Browser(var location: URI) {
        private val client = JerseyClientBuilder().build()
            .property(ClientProperties.FOLLOW_REDIRECTS, false)

        private val cookies = mutableMapOf<String, String>()

        fun get() = request { get() }

        fun post() = request { post(null) }

        fun request(verb: JerseyInvocation.Builder.() -> Response): Response {
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
        val browser = Browser(URI("http://localhost:${APP.localPort}/api/auth/gh"))

        // Begin process
        browser.get()

        // Query GitHub auth - the stubbing emulates the process which ends in browser being sent to trampoline
        browser.get()

        // Hit trampoline
        browser.get()

        // Extract fragment query params
        val match = ".*code=(.*)&state=(.*)".toRegex().matchEntire(browser.location.query)!!
        val code = match.groups[1]!!.value
        val state = match.groups[2]!!.value

        // Finish auth
        browser.location = URIBuilder("http://localhost:${APP.localPort}/api/auth/gh/complete")
            .setParameter("code", code)
            .setParameter("state", state)
            .build()

        val xsrfToken = browser.post()
            .headers[FrontendAuthStrategy.XSRF_TOKEN_HEADER]!!.last() as String

        // Attempt to get protected resource
        browser.location = URI("http://localhost:${APP.localPort}/api/builds")
        val response = browser.request {
            header(FrontendAuthStrategy.XSRF_TOKEN_HEADER, xsrfToken).get()
        }

        with(response) {
            assertThat(familyOf(status), equalTo(SUCCESSFUL))
        }
    }

    companion object {
        private val CODEC = SecretsCodec(DEV_MASTER_KEY_BASE64)

        private val CLIENT_ID = "foo"
        private val CLIENT_SECRET = UnsafeSecret("bar")
        private val CODE = "good"
        private val ACCESS_TOKEN = UUID.randomUUID().toString()
        private val CUSTOMER_ID = CustomerId(4321)

        @JvmField
        @ClassRule
        val github = WireMockRule(wireMockConfig()
            .dynamicPort()
            .extensions(ResponseTemplateTransformer(false))
        )

        @JvmField
        @ClassRule
        val registry = WireMockRule(wireMockConfig().dynamicPort())

        @JvmField
        @ClassRule
        val eval = WireMockRule(wireMockConfig().dynamicPort())

        /**
         * This shouldn't be needed, but the compiler gets in a twist if you pass
         * { "http://localhost:${APP.localPort}" } as an arg to the APP construction below.
         * So instead we pass it the address for this proxy, which in turn forwards requests back to the proxy
         * endpoint :/
         */
        @JvmField
        @ClassRule
        val trampolineProxy = WireMockRule(wireMockConfig().dynamicPort())

        @ClassRule
        @JvmField
        val APP = DropwizardAppRule<HomeConfiguration>(
            HomeApplication::class.java,
            resourceFilePath("test.yml"),
            config("cookies.signing_key_encrypted_base64", CODEC.encrypt(TOKEN_KEY_BASE64).somewhatUnsafe),
            config("github.trampoline_url", { "http://localhost:${trampolineProxy.port()}/api/auth/gh/callback" }),
            config("github.oauth_api_root", { "http://localhost:${github.port()}" }),
            config("github.api_root", { "http://localhost:${github.port()}" }),
            config("github.client_id", CLIENT_ID),
            config("github.client_secret_encrypted", CODEC.encrypt(CLIENT_SECRET).somewhatUnsafe),
            config("github.redirect_host", { "http://localhost:${github.port()}" }),
            config("registry_url", { "http://localhost:${registry.port()}/api" }),
            config("eval_url", { "http://localhost:${eval.port()}/api" })
        )
    }
}
