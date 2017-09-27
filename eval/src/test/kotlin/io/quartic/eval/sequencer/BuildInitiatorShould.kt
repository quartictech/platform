package io.quartic.eval.sequencer

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Test
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture

class BuildInitiatorShould {
    @Test
    fun get_customer() {
        runBlocking {
            initiator.start(trigger)
        }

        verify(registry).getCustomerAsync(null, repoId)
    }

    @Test
    fun insert_build() {
        runBlocking {
            initiator.start(trigger)
        }

        verify(database).insertBuild(buildId, customer.id, "develop")
    }

    @Test
    fun not_insert_build_for_nonexistent_customer() {
        whenever(registry.getCustomerAsync(null, repoId)).thenReturn(completedFuture(null))
        val build = runBlocking {
            initiator.start(trigger)
        }

        verifyZeroInteractions(database)
        assertThat(build, nullValue())
    }

    private val customerId = CustomerId(100L)
    private val repoId = 777L
    private val buildId = UUID(0, 100)
    private val branch = "develop"
    private val build = Database.BuildStatusRow(buildId, 100, branch, customerId, "running", null, null)

    private val trigger = mock<BuildTrigger.GithubWebhook> {
        on { repoId } doReturn repoId
        on { ref } doReturn "refs/heads/develop"
        on { branch() } doReturn branch
    }

    private val customer = mock<Customer> {
        on { id } doReturn customerId
    }

    private val registry = mock<RegistryServiceClient> {
        on { getCustomerAsync(anyOrNull(), eq(repoId)) } doReturn completedFuture(customer)
    }

    private val database = mock<Database> {
        on { getBuild(any()) } doReturn build
    }

    private val initiator = BuildInitiator(database, registry, uuidGen = { buildId })
}
