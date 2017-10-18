package io.quartic.common.application

import io.dropwizard.auth.Auth
import io.dropwizard.setup.Environment
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.auth.frontend.FrontendTokenGenerator
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.TOKEN_COOKIE
import io.quartic.common.auth.frontend.FrontendAuthStrategy.Companion.XSRF_TOKEN_HEADER
import io.quartic.common.auth.frontend.FrontendUser
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.test.TOKEN_KEY_BASE64
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.time.Duration
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.HttpHeaders

class ApplicationBaseFrontendAuthShould {

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
        val tokenGenerator = FrontendTokenGenerator(
            RULE.configuration.auth as FrontendAuthConfiguration,
            CODEC,
            Duration.ofMinutes(10)
        )

        val tokens = tokenGenerator.generate(FrontendUser(666, 777), "localhost")

        val response = target()
            .request()
            .cookie(TOKEN_COOKIE, tokens.jwt)
            .header(XSRF_TOKEN_HEADER, tokens.xsrf)
            .get()

        assertThat(response.status, equalTo(200))
        assertThat(response.readEntity(String::class.java), equalTo("Hello 666"))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/test")

    class TestConfiguration : ConfigurationBase()

    class TestApplication : ApplicationBase<TestConfiguration>() {
        @Path("/test")
        class TestResource {
            @GET fun get(@Auth user: FrontendUser) = "Hello ${user.name}"
        }

        override fun runApplication(configuration: TestConfiguration, environment: Environment) {
            environment.jersey().register(TestResource())
        }
    }

    companion object {
        private val CODEC = SecretsCodec(DEV_MASTER_KEY_BASE64)

        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml"),
            config("auth.type", "external"),
            config("auth.key_encrypted_base64", CODEC.encrypt(TOKEN_KEY_BASE64).somewhatUnsafe)
        )
    }
}
