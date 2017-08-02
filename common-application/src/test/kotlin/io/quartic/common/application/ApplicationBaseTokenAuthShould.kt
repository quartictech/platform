package io.quartic.common.application

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.auth.TokenAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.TokenAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.TokenGenerator
import io.quartic.common.auth.User
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.time.Duration
import javax.ws.rs.core.HttpHeaders

class ApplicationBaseTokenAuthShould {

    @Test
    fun respond_with_401_if_no_token_supplied() {
        val response = target()
            .request()
            .get()

        assertThat(response.status, equalTo(401))
        assertThat(response.headers[HttpHeaders.WWW_AUTHENTICATE]!!.last() as String, startsWith("Cookie"))
    }

    @Test
    fun respond_with_200_if_valid_token_supplied() {
        val tokenGenerator = TokenGenerator(KEY, Duration.ofMinutes(10))

        val tokens = tokenGenerator.generate(User(666, 777), "localhost")

        val response = target()
            .request()
            .cookie(TOKEN_COOKIE, tokens.jwt)
            .header(XSRF_TOKEN_HEADER, tokens.xsrf)
            .get()

        assertThat(response.status, equalTo(200))
        assertThat(response.readEntity(String::class.java), equalTo("Hello 666"))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/test")

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestApplication.TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml"),
            config("auth.type", "token"),
            config("auth.base64EncodedKey", KEY)
        )
    }
}
