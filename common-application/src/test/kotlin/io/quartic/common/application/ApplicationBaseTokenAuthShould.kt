package io.quartic.common.application

import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.auth.JwtGenerator
import io.quartic.common.auth.JwtId
import io.quartic.common.uid.randomGenerator
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import javax.ws.rs.core.HttpHeaders

class ApplicationBaseTokenAuthShould {

    @Test
    fun respond_with_401_if_no_token_supplied() {
        val response = target()
            .request()
            .get()

        assertThat(response.status, equalTo(401))
    }

    @Test
    fun respond_with_200_if_valid_token_supplied() {
        val jwtGenerator = JwtGenerator(
            KEY,
            Duration.ofMinutes(10),
            Clock.systemUTC(),
            randomGenerator(::JwtId)
        )

        val response = target()
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtGenerator.generate("oliver")}")
            .get()

        assertThat(response.status, equalTo(200))
        assertThat(response.readEntity(String::class.java), equalTo("Hello oliver"))
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
