package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.quartic.qube.api.Response
import io.quartic.qube.pods.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import org.junit.Test
import java.util.*
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED

class OrchestratorShould {
    private val events = Channel<QubeEvent>(UNLIMITED)
    private val podKey = PodKey(UUID.randomUUID(), "test")
    private val returnChannel = Channel<Response>()

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
    fun ensure_pods_are_cancelled() {
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

    fun createPod() = QubeEvent.CreatePod(podKey, returnChannel, "dummy:1", listOf("true"))
    fun cancelPod() = QubeEvent.CancelPod(podKey)
    fun createClient() = QubeEvent.CreateClient(podKey.client)
    fun cancelClient() = QubeEvent.CancelClient(podKey.client)
}
