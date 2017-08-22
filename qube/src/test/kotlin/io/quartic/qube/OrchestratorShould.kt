package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.quartic.qube.api.Response
import io.quartic.qube.pods.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import org.junit.Test
import java.util.*

class OrchestratorShould {
    private val events = Channel<QubeEvent>()
    private val podKey = PodKey(UUID.randomUUID(), "test")
    private val returnChannel = Channel<Response>()

    private val worker = mock<Worker>()
    private val orchestratorState = OrchestratorState()
    private val orchestrator = Orchestrator(events, worker, 1, orchestratorState)

    @Test
    fun enqueue_pods() {
        runBlocking {
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            verify(worker, timeout(500)).run(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun ignore_pods_for_nonexistent_scope() {
        runBlocking {
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createPod())
            verify(worker, timeout(500).times(0)).run(createPod())

            reset(worker)
            events.send(createClient())
            events.send(createPod())
            verify(worker, timeout(500)).run(createPod())

            reset(worker)
            events.send(cancelClient())
            events.send(createPod())
            verify(worker, timeout(500).times(0)).run(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun respect_concurrency() {
        runBlocking {
            whenever(worker.run(any())).then { runBlocking { delay(1000) } }
            val deferred = async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            events.send(createPod())
            verify(worker, timeout(500).times(1)).run(createPod())
            deferred.cancel()
        }
    }

    @Test
    fun cancel_pods() {
        runBlocking {
            whenever(worker.run(any())).then {
                launch(CommonPool) { delay(1000) }
            }
            async(CommonPool) { orchestrator.run() }
            events.send(createClient())
            events.send(createPod())
            delay(100)
            events.send(cancelPod())
            verify(worker, timeout(500)).run(any())
        }
    }

    fun createPod() = QubeEvent.CreatePod(podKey, returnChannel, "dummy:1", listOf("true"))
    fun createClient() = QubeEvent.CreateClient(podKey.client)
    fun cancelClient() = QubeEvent.CancelClient(podKey.client)
    fun cancelPod() = QubeEvent.CancelPod(podKey)
}
