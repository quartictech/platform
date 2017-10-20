package io.quartic.common.application

import io.dropwizard.auth.Auth
import io.dropwizard.setup.Environment
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.auth.legacy.LegacyUser
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.GET
import javax.ws.rs.Path

class ApplicationBaseLegacyAuthShould {

    @Test
    fun use_legacy_auth_by_default() {
        val response = target()
            .request()
            .get()

        assertThat(response.status, equalTo(200))
    }

    @Test
    fun use_x_forwarded_user_as_identity() {
        val response = target()
            .request()
            .header("X-Forwarded-User", "noob")
            .get()

        assertThat(response.readEntity(String::class.java), equalTo("Hello noob"))
    }

    private fun target() = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/test")

    class TestConfiguration : ConfigurationBase()

    class TestApplication : ApplicationBase<TestConfiguration>() {
        @Path("/test")
        class TestResource {
            @GET fun get(@Auth user: LegacyUser) = "Hello ${user.name}"
        }

        override fun runApplication(configuration: TestConfiguration, environment: Environment) {
            environment.jersey().register(TestResource())
        }
    }

    companion object {
        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml")
        )
    }
}
