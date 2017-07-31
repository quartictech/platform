package io.quartic.orf

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.application.TokenAuthConfiguration
import io.quartic.common.auth.TokenAuthStrategy
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenAuthStrategy.Tokens
import io.quartic.common.auth.User
import io.quartic.mgmt.MgmtApplication
import io.quartic.mgmt.MgmtConfiguration
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasKey
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class OrfApplicationShould {

    @Test
    fun generate_response_with_correct_header_and_cookie() {
        val response = target()
            .queryParam("userId", "oliver")
            .request()
            .get()

        assertThat(response.status, equalTo(200))
        assertThat(response.cookies, hasKey(TOKEN_COOKIE))
        assertThat(response.headers, hasKey(XSRF_TOKEN_HEADER))

        assertTrue(response.cookies[TOKEN_COOKIE]!!.isSecure)
        assertTrue(response.cookies[TOKEN_COOKIE]!!.isHttpOnly)

        val authStrategy = TokenAuthStrategy(TokenAuthConfiguration(KEY))
        val tokens = Tokens(
            response.cookies[TOKEN_COOKIE]!!.value,
            response.headers[XSRF_TOKEN_HEADER]!!.last() as String,
            "localhost:${RULE.localPort}"
        )

        assertThat(authStrategy.authenticate(tokens), equalTo(User("oliver")))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/token")

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<MgmtConfiguration>(
            MgmtApplication::class.java,
            resourceFilePath("test.yml"),
            config("auth.type", "token"),
            config("auth.base64EncodedKey", KEY)
        )
    }
}
