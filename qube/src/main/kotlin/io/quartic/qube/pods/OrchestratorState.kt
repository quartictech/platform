package io.quartic.qube.pods

import io.quartic.common.logging.logger
import kotlinx.coroutines.experimental.Job
import java.util.*

class OrchestratorState {
    private val LOG by logger()
    private val clients = mutableSetOf<UUID>()
    val runningPods = mutableMapOf<PodKey, Job>()
    private val waitingList: Queue<QubeEvent.CreatePod> = LinkedList<QubeEvent.CreatePod>()

    fun createPod(message: QubeEvent.CreatePod) = waitingList.add(message)
    fun createClient(message: QubeEvent.CreateClient) = clients.add(message.client)
    fun cancelRunningPod(key: PodKey) {
        runningPods[key]?.cancel()
        runningPods.remove(key)
    }

    fun cancelAll() {
        runningPods.forEach { key, job -> job.cancel() }
        runningPods.clear()
    }

    fun cancelClient(client: UUID) {
        runningPods
            .filterKeys { key -> key.client == client }
            .forEach { key, job -> cancelRunningPod(key) }
        clients.remove(client)
    }

    fun removeRunningPod(key: PodKey) = runningPods.remove(key)

    private fun podCanRun(key: PodKey) = clients.contains(key.client) && ! runningPods.containsKey(key)

    suspend fun drainWaitingList(concurrency: Int, f: (create: QubeEvent.CreatePod) -> Job) {
        while (waitingList.isNotEmpty() && runningPods.size < concurrency) {
            val create = waitingList.remove()
            if (podCanRun(create.key)) {
                val job =  f(create)
                runningPods.put(create.key, job)
            } else {
                LOG.warn("Ignoring event: {}", create)
            }
        }
    }
}
