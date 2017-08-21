package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.quartic.qube.api.Response
import io.quartic.qube.pods.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.*

class OrchestratorShould {
    private val events = Channel<QubeEvent>()
    private val podKey = PodKey(UUID.randomUUID(), "test")
    private val returnChannel = Channel<Response>()

    private val worker = mock<Worker>()
    private val orchestrator = Orchestrator(events, worker, 1)

    @Test
    fun enqueue_pods() {
        runBlocking {
            async(CommonPool) { orchestrator.run() }
            events.send(createScope())
            events.send(createPod())
            verify(worker, timeout(500)).run(createPod())
        }
    }

    @Test
    fun ignore_pods_for_nonexistent_scope() {
        runBlocking {
            async(CommonPool) { orchestrator.run() }
            events.send(createPod())
            verify(worker, timeout(500).times(0)).run(createPod())

            reset(worker)
            events.send(createScope())
            events.send(createPod())
            verify(worker, timeout(500)).run(createPod())

            reset(worker)
            events.send(cancelScope())
            events.send(createPod())
            verify(worker, timeout(500).times(0)).run(createPod())
        }
    }

    @Test
    fun respect_concurrency() {
        runBlocking {
            whenever(worker.run(any())).then { runBlocking { delay(1000) } }
            async(CommonPool) { orchestrator.run() }
            events.send(createScope())
            events.send(createPod())
            events.send(createPod())
            verify(worker, timeout(500).times(1)).run(createPod())
        }
    }

    fun createPod() = QubeEvent.CreatePod(podKey, returnChannel, "dummy:1", listOf("true"))
    fun createScope() = QubeEvent.CreateClient(podKey.client)
    fun cancelScope() = QubeEvent.CancelScope(podKey.client)
}
