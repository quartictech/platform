package io.quartic.bild

import com.nhaarman.mockito_kotlin.*
import io.quartic.bild.api.model.TriggerDetails
import io.quartic.bild.model.BuildId
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.resource.TriggerResource
import io.quartic.bild.store.JobRecord
import io.quartic.bild.store.JobResultStore
import io.quartic.common.model.CustomerId
import io.quartic.common.uid.UidGenerator
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture


class TriggerResourceShould {
    private val dag = mapOf("noob" to "yes")
    private val jobResults = mock<JobResultStore>()
    private val queue = mock<BlockingQueue<BuildJob>>()
    private val idGenerator = mock<UidGenerator<BuildId>>()
    private val registry = mock<RegistryServiceClient>()
    private val resource = TriggerResource(queue, registry, idGenerator)
    private val bildId = BuildId("noob")

    private val laDispute = Customer(
        CustomerId(1L),
        123L,
        123456L,
        "la dispute",
        "ladispute",
        "ladispute"
    )

    init {
        whenever(jobResults.getLatest(CustomerId("111")))
            .thenReturn(JobRecord(null, dag))

        whenever(idGenerator.get()).thenReturn(bildId)
        whenever(registry.getCustomer(anyOrNull(), anyOrNull()))
            .thenReturn(CompletableFuture.completedFuture(null))

        whenever(registry.getCustomer(anyOrNull(), eq(123456L)))
            .thenReturn(CompletableFuture.completedFuture(laDispute))

    }

    @Test
    fun trigger_queries_registry_and_enqueues() {
        resource.trigger(TriggerDetails(
            "wat",
            "wat",
            0L,
            123456L,
            "https://no",
            "wat",
            "hash",
            Instant.now()
        ))

        verify(registry).getCustomer(isNull(), eq(123456L))
        verify(queue).put(BuildJob(
            bildId,
            laDispute.id,
            0L,
            "https://no",
            "wat",
            "hash",
            BuildPhase.TEST
        ))
    }

    @Test
    fun trigger_does_nothing_if_repo_not_found() {
        resource.trigger(TriggerDetails(
            "wat",
            "wat",
            0L,
            123L,
            "https://no",
            "wat",
            "hash",
            Instant.now()
        ))

        verify(registry).getCustomer(isNull(), eq(123L))
        verify(queue, never()).put(any())
    }
}
