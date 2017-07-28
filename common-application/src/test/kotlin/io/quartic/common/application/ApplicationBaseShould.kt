package io.quartic.common.application

import io.dropwizard.setup.Environment
import io.dropwizard.testing.ConfigOverride
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
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.HttpHeaders


class ApplicationBaseShould {

    @Path("/test")
    @PermitAll
    class TestResource {
        @GET fun get() = "Hello world"
    }

    class TestConfiguration : ConfigurationBase()

    class TestApplication : ApplicationBase<TestConfiguration>(true) {
        override fun runApplication(configuration: TestConfiguration, environment: Environment) {
            environment.jersey().register(TestResource())
        }
    }

    @Test
    fun respond_with_401_if_no_token_supplied() {
        val client = JerseyClientBuilder().build()

        val response = client.target("http://localhost:${RULE.localPort}/api/test")
            .request()
            .get()

        println(response.headers)

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

        val client = JerseyClientBuilder().build()

        val response = client.target("http://localhost:${RULE.localPort}/api/test")
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtGenerator.generate("oliver")}")
            .get()

        assertThat(response.status, equalTo(200))
    }

    companion object {
        private val KEY = "BffwOJzi7ejTe9yC1IpQ4+P6fYpyGz+GvVyrfhamNisNqa96CF8wGSp3uATaITUP7r9n6zn9tDN8k4424zwZ2Q=="

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml"),
            ConfigOverride.config("base64EncodedJwtKey", KEY)
        )
    }
}
