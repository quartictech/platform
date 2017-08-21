package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.quartic.common.logging.logger
import io.quartic.qube.api.SentMessage
import io.quartic.qube.qube.KubernetesClient
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.selects.select
import java.util.*

class Orchestrator(
    private val client: KubernetesClient,
    private val events: Channel<QubeEvent>,
    private val podTemplate: Pod
) {
    private val LOG by logger()

    suspend fun createPod(pod: Pod) = async(newSingleThreadContext("createPod")) {
        client.createPod(pod)
        LOG.info("[{}] Pod created", pod.metadata.name)
    }

    suspend fun deletePod(podName: String) = async(newSingleThreadContext("deletePod")) {
        client.deletePod(podName)
        LOG.info("[{}] Pod deleted", podName)
    }

    suspend fun runPod(podName: String, channel: Channel<Pod>, sendMessage: Channel<SentMessage>) {
       for (message in channel) {
           val state = message.status.containerStatuses.firstOrNull()?.state

           if (state != null) {
               sendMessage.send(SentMessage.PodStatus(podName, state.toString()))
               if (state.terminated != null) {
                   return
               }
           }
       }
    }

    private suspend fun runWorker(create: QubeEvent.CreatePod) {
        val podName = "${create.key.scope}-${create.key.name}"
        val podEvents = Channel<Pod>()
        val watch = client.watchPod(podName) { _, pod ->
            if (pod != null) {
                podEvents.offer(pod)
            }
        }

        try {
            createPod(PodBuilder(podTemplate)
                .editOrNewMetadata()
                .withName(podName)
                .endMetadata()
                .build())
                .await()

            runPod(podName, podEvents, create.returnChannel)
        }
        finally {
            deletePod(podName)
            LOG.info("[{}] Pod deleted", podName)
            watch.close()
        }
    }

    fun run() = launch(CommonPool) {
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
                    runningPods.forEach{ key, job ->
                        if (key.scope == message.scope) {
                            job.cancel()
                            runningPods.remove(key)
                        }
                    }

                    scopes.remove(message.scope)
                }

                is QubeEvent.PodTerminated -> {
                    LOG.info("Removing pod: {}", message)
                    runningPods.remove(message.key)
                }
            }

            // Drain waiting list
            while (waitingList.isNotEmpty() && runningPods.size < MAX_CONCURRENCY) {
                val create = waitingList.remove()
                if (scopes.contains(create.key.scope) &&
                    !runningPods.containsKey(create.key)) {
                    LOG.info("Running {}", create.key.name)
                    val job = async(coroutineContext) { runWorker(create) }
                    runningPods.put(create.key, job)
                } else {
                    LOG.warn("Discarding request: {}", create)
                }
            }
        }
    }

    companion object {
        val MAX_CONCURRENCY = 4
    }
}
