package io.quartic.eval.sequencer

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.eval.database.model.TriggerReceived
import io.quartic.eval.database.model.toDatabaseModel
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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

        verify(database).createBuild(uuid, uuid, customer.id, trigger, Instant.MIN)
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
    private val uuid = UUID(0, 100)
    private val branch = "develop"
    private val build = Database.BuildRow(uuid, 100, branch, customerId,
        "running", Instant.now(), mock())

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
        on { createBuild(any(), any(), any(), any(), any()) } doReturn build
    }

    private val initiator = BuildInitiator(database, registry,
        uuidGen = { uuid },
        clock = Clock.fixed(Instant.MIN, ZoneId.systemDefault()))
}
