package io.quartic.qube.pods

import io.quartic.common.logging.logger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.selects.select
import java.util.*

class Orchestrator(
    private val events: Channel<QubeEvent>,
    private val worker: Worker,
    private val concurrency: Int = 4
) {
    private val LOG by logger()

    suspend fun run() {
        val scopes = mutableSetOf<UUID>()
        val runningPods = mutableMapOf<PodKey, Job>()
        val waitingList: Queue<QubeEvent.CreatePod> = LinkedList<QubeEvent.CreatePod>()

        while (true) {
            val message = select<QubeEvent> {
                events.onReceive { it }
                runningPods.forEach { key, job ->
                    job.onJoin { QubeEvent.PodTerminated(key) }
                }
            }

            LOG.info("Message received {}", message)
            when (message) {
                is QubeEvent.CreatePod -> {
                    waitingList.add(message)
                }
                is QubeEvent.CreateScope -> {
                    scopes.add(message.scope)
                }
                is QubeEvent.CancelPod -> {
                    runningPods[message.key]?.cancel()
                    runningPods.remove(message.key)
                }

                is QubeEvent.CancelScope -> {
                    val removeKeys = mutableSetOf<PodKey>()
                    runningPods.forEach{ key, job ->
                        if (key.scope == message.scope) {
                            job.cancel()
                            removeKeys.add(key)
                        }
                    }

                    removeKeys.forEach{key -> runningPods.remove(key)}
                    scopes.remove(message.scope)
                }

                is QubeEvent.PodTerminated -> {
                    LOG.info("Removing pod: {}", message)
                    runningPods.remove(message.key)
                }
            }

            // Drain waiting list
            while (waitingList.isNotEmpty() && runningPods.size < concurrency) {
                val create = waitingList.remove()
                if (scopes.contains(create.key.scope) &&
                    !runningPods.containsKey(create.key)) {
                    LOG.info("Running {}", create.key.name)
                    val job = async(CommonPool) { worker.run(create) }
                    runningPods.put(create.key, job)
                } else {
                    LOG.warn("Discarding request: {}", create)
                }
            }
        }
    }
}
