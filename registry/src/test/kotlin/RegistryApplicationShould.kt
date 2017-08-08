package io.quartic.mgmt

import feign.FeignException
import io.dropwizard.testing.ConfigOverride.config
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.client.client
import io.quartic.common.model.CustomerId
import io.quartic.common.test.MASTER_KEY_BASE64
import io.quartic.common.test.assertThrows
import io.quartic.registry.RegistryApplication
import io.quartic.registry.RegistryConfiguration
import io.quartic.registry.api.RegistryService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.ClassRule
import org.junit.Test

class RegistryApplicationShould {
    val client = client<RegistryService>(this::class.java, "http://localhost:${APP.localPort}/api/")

    @Test
    fun finds_test_customer() {
        val customer = client.getCustomerById(CustomerId(111))
        assertThat(customer, notNullValue())
    }

    @Test
    fun throws_404_when_customer_not_found() {
        val e = assertThrows<FeignException> {
            client.getCustomerById(CustomerId(122))
        }

        assertThat(e.status(), equalTo(404))
    }

    companion object {
        @ClassRule
        @JvmField
        val APP = DropwizardAppRule<RegistryConfiguration>(
            RegistryApplication::class.java,
            resourceFilePath("test.yml"),
            config("masterKeyBase64", MASTER_KEY_BASE64.veryUnsafe)
        )
    }
}
