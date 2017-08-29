package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.model.ContainerSpec
import io.quartic.qube.pods.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import org.junit.Test
import java.util.*
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED

class OrchestratorShould {
    private val events = Channel<QubeEvent>(UNLIMITED)
    private val podKey = PodKey(UUID.randomUUID(), "test")
    private val returnChannel = Channel<QubeResponse>()

    private val worker = mock<Worker>()
    private val orchestratorState = OrchestratorState()
    private val orchestrator = Orchestrator(events, worker, 1, orchestratorState)

    init {
        whenever(worker.runAsync(any())).then { async(CommonPool) { delay(100) } }
    }

    @Test
    fun enqueue_pods() {
        runBlocking {
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            verify(worker, timeout(500)).runAsync(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun ignore_pods_for_nonexistent_scope() {
        runBlocking {
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createPod())
            verify(worker, timeout(500).times(0)).runAsync(createPod())

            reset(worker)
            events.send(createClient())
            events.send(createPod())
            verify(worker, timeout(500)).runAsync(createPod())

            reset(worker)
            events.send(cancelClient())
            events.send(createPod())
            verify(worker, timeout(500).times(0)).runAsync(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun respect_concurrency() {
        runBlocking {
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            events.send(createPod())
            verify(worker, timeout(500).times(1)).runAsync(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun cancel_pods() {
        val job = mock<Job>()
        runBlocking {
            whenever(worker.runAsync(any())).thenReturn(job)
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            events.send(cancelPod())
            verify(worker, timeout(500)).runAsync(createPod())
            verify(job, timeout(500)).cancel(anyOrNull())
            deferred.cancel()
        }
    }

    @Test
    fun handle_worker_exception() {
        runBlocking {
            val job = async(CommonPool) {
                throw RuntimeException("something was weird")
            }
            whenever(worker.runAsync(any())).thenReturn(job)
            async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            events.send(createPod())
            verify(worker, timeout(1000).times(2)).runAsync(any())
        }
    }

    fun createPod() = QubeEvent.CreatePod(podKey, returnChannel,
        ContainerSpec("dummy:1", listOf("true"), 8000))
    fun cancelPod() = QubeEvent.CancelPod(podKey)
    fun createClient() = QubeEvent.CreateClient(podKey.client)
    fun cancelClient() = QubeEvent.CancelClient(podKey.client)
}
