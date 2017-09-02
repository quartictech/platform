package io.quartic.registry

import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.client.ClientBuilder
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.registry.api.RegistryServiceClient
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.ClassRule
import org.junit.Test
import retrofit2.HttpException
import java.util.concurrent.CompletionException

class RegistryApplicationShould {
    private val client = ClientBuilder(this).retrofit<RegistryServiceClient>("http://localhost:${APP.localPort}/api/")

    @Test
    fun finds_test_customer() {
        val customer = client.getCustomerByIdAsync(CustomerId(111)).join()
        assertThat(customer, notNullValue())
    }

    @Test
    fun throws_404_when_customer_not_found() {
        val e = assertThrows<CompletionException> {
            client.getCustomerByIdAsync(CustomerId(122)).join()
        }

        assertThat((e.cause as HttpException).code(), equalTo(404))
    }

    @Test
    fun find_customer_by_subdomain() {
        val customer = client.getCustomerAsync("localhost", null).join()
        assertThat(customer!!.name, equalTo("Noobhole"))
        val customer2 = client.getCustomerAsync("remotehost", null)
        val e = assertThrows<CompletionException> { customer2.join() }
        assertThat((e.cause as HttpException).code(), equalTo(404))
    }

    @Test
    fun find_customer_by_repo() {
        val customer = client.getCustomerAsync(null, 555).join()
        assertThat(customer!!.name, equalTo("Noobhole"))
        val customer2 = client.getCustomerAsync(null, 666)
        val e = assertThrows<CompletionException> { customer2.join() }
        assertThat((e.cause as HttpException).code(), equalTo(404))
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
