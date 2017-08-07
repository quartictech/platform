package io.quartic.common.application

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.MASTER_KEY
import io.quartic.common.secrets.encodeAsBase64
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test

class ApplicationBaseDummyAuthShould {

    @Test
    fun do_dummy_auth_by_default() {
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

    companion object {
        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<TestApplication.TestConfiguration>(
            TestApplication::class.java,
            resourceFilePath("test.yml"),
            ConfigOverride.config("base64EncodedMasterKey", MASTER_KEY.encodeAsBase64())
        )
    }
}
