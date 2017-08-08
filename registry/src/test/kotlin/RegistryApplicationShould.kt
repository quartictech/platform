package io.quartic.mgmt

import feign.FeignException
import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.client.client
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.registry.RegistryApplication
import io.quartic.registry.RegistryConfiguration
import io.quartic.registry.api.RegistryService
import org.junit.ClassRule
import org.junit.Test

class RegistryApplicationShould {
    val client = client<RegistryService>(this::class.java, "http://localhost:${APP.localPort}")

    @Test
    fun throws_404_when_customer_not_found() {
        assertThrows<FeignException> {
            client.getCustomerById(CustomerId(122))
        }
    }

    companion object {
        @ClassRule
        @JvmField
        val APP = DropwizardAppRule<RegistryConfiguration>(
            RegistryApplication::class.java,
            resourceFilePath("test.yml")
        )
    }
}
