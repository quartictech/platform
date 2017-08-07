package io.quartic.bild

import com.nhaarman.mockito_kotlin.*
import io.quartic.bild.api.model.TriggerDetails
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.resource.TriggerResource
import io.quartic.common.model.CustomerId
import io.quartic.common.uid.UidGenerator
import io.quartic.registry.api.RegistryServiceAsync
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture


class TriggerResourceShould {
    private val dag = mapOf("noob" to "yes")
    private val jobResults = mock<JobResultStore>()
    private val queue = mock<BlockingQueue<BildJob>>()
    private val idGenerator = mock<UidGenerator<BildId>>()
    private val registry = mock<RegistryServiceAsync>()
    private val resource = TriggerResource(queue, registry, idGenerator)
    private val bildId = BildId("noob")

    private val laDispute = Customer(
        1L,
        123L,
        123456L,
        "la dispute",
        "ladispute",
        "ladispute"
    )

    init {
        whenever(jobResults.getLatest(CustomerId("111")))
            .thenReturn(JobResultStore.Record(null, dag))

        whenever(idGenerator.get()).thenReturn(bildId)
        whenever(registry.getCustomerAsync(anyOrNull(), anyOrNull()))
            .thenReturn(CompletableFuture.completedFuture(null))

        whenever(registry.getCustomerAsync(anyOrNull(), eq(123456L)))
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

        verify(registry).getCustomerAsync(isNull(), eq(123456L))
        verify(queue).put(BildJob(
            bildId,
            CustomerId(laDispute.id),
            0L,
            "https://no",
            "wat",
            "hash",
            BildPhase.TEST
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

        verify(registry).getCustomerAsync(isNull(), eq(123L))
        verify(queue, never()).put(any())
    }
}
