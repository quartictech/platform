package io.quartic.registry

import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import io.quartic.common.client.retrofitClient
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.registry.RegistryApplication
import io.quartic.registry.RegistryConfiguration
import io.quartic.registry.api.RegistryServiceClient
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.ClassRule
import org.junit.Test
import retrofit2.HttpException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

class RegistryApplicationShould {
    val client = retrofitClient<RegistryServiceClient>(this::class.java, "http://localhost:${APP.localPort}/api/")

    @Test
    fun finds_test_customer() {
        val customer = client.getCustomerById(CustomerId(111)).join()
        assertThat(customer, notNullValue())
    }

    @Test
    fun throws_404_when_customer_not_found() {
        val e = assertThrows<CompletionException> {
            client.getCustomerById(CustomerId(122)).join()
        }

        assertThat((e.cause as HttpException).code(), equalTo(404))
    }

    @Test
    fun find_customer_by_subdomain() {
        val customer = client.getCustomer("localhost", null).join()
        assertThat(customer!!.name, equalTo("Noobhole"))
        val customer2 = client.getCustomer("remotehost", null)
        val e = assertThrows<CompletionException> { customer2.join() }
        assertThat((e.cause as HttpException).code(), equalTo(404))
    }

    @Test
    fun find_customer_by_repo() {
        val customer = client.getCustomer(null, 555).join()
        assertThat(customer!!.name, equalTo("Noobhole"))
        val customer2 = client.getCustomer(null, 666)
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
