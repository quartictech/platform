package io.quartic.qube.pods

import io.quartic.common.logging.logger
import kotlinx.coroutines.experimental.Job
import java.util.*

class OrchestratorState {
    private val LOG by logger()
    private val clients = mutableSetOf<UUID>()
    private val _runningPods = mutableMapOf<PodKey, Job>()
    val runningPods: Map<PodKey, Job> = _runningPods
    private val waitingList: Queue<QubeEvent.CreatePod> = LinkedList<QubeEvent.CreatePod>()

    fun createPod(message: QubeEvent.CreatePod) = waitingList.add(message)
    fun createClient(message: QubeEvent.CreateClient) = clients.add(message.client)
    fun cancelRunningPod(key: PodKey) {
        _runningPods[key]?.cancel()
        _runningPods.remove(key)
    }

    fun cancelAll() {
        _runningPods.forEach { _, job -> job.cancel() }
        _runningPods.clear()
        clients.clear()
        waitingList.clear()
    }

    fun cancelClient(client: UUID) {
        runningPods
            .filterKeys { key -> key.client == client }
            .forEach { key, _ -> cancelRunningPod(key) }
        clients.remove(client)
    }

    fun removeRunningPod(key: PodKey) = _runningPods.remove(key)

    private fun podCanRun(key: PodKey) = clients.contains(key.client) && ! _runningPods.containsKey(key)

    suspend fun drainWaitingList(concurrency: Int, f: (create: QubeEvent.CreatePod) -> Job) {
        while (waitingList.isNotEmpty() && _runningPods.size < concurrency) {
            val create = waitingList.remove()
            if (podCanRun(create.key)) {
                val job =  f(create)
                _runningPods.put(create.key, job)
            } else {
                LOG.warn("Ignoring event: {}", create)
            }
        }
    }
}
