package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.qube.api.model.TriggerDetails
import io.quartic.qube.model.BuildId
import io.quartic.qube.model.BuildJob
import io.quartic.qube.model.BuildPhase
import io.quartic.qube.resource.TriggerResource
import io.quartic.qube.store.BuildStore
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture

class TriggerResourceShould {
    private val buildStore = mock<BuildStore>()
    private val queue = mock<BlockingQueue<BuildJob>>()
    private val registry = mock<RegistryServiceClient>()
    private val resource = TriggerResource(queue, registry, buildStore)
    private val buildId = BuildId("noob")

    private val laDispute = Customer(
        CustomerId(1L),
        123L,
        123456L,
        "la dispute",
        "ladispute",
        "ladispute"
    )

    init {
        whenever(buildStore.createBuild(any(), any(), any(), any(), any(), any()))
            .thenReturn(buildId)

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
            URI("https://no"),
            "wat",
            "hash",
            Instant.now()
        ))

        verify(registry).getCustomerAsync(isNull(), eq(123456L))
        verify(queue).put(BuildJob(
            buildId,
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
            URI("https://no"),
            "wat",
            "hash",
            Instant.now()
        ))

        verify(registry).getCustomerAsync(isNull(), eq(123L))
        verify(queue, never()).put(any())
    }
}
