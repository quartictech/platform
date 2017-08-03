package io.quartic.bild

import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Test


class BildApplicationShould {
    @Test
    fun fetch_dag_for_customer() {
        val response = target("111")
            .request()
            .get()

        assertThat(response.status, Matchers.equalTo(200))
    }

    @Test
    fun raise_404_if_non_existant() {
        val response = target("ladispute")
            .request()
            .get()

        assertThat(response.status, Matchers.equalTo(404))
    }

    private fun target(customerId: String) = JerseyClientBuilder().build().target("http://localhost:${RULE.localPort}/api/dag/${customerId}")

    companion object {
        @ClassRule
        @JvmField
        val RULE = DropwizardAppRule<BildConfiguration>(
            BildApplication::class.java,
            resourceFilePath("test.yml")
        )
    }
}
