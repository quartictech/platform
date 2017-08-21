package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.quartic.common.logging.logger
import io.quartic.qube.api.SentMessage
import io.quartic.qube.api.Status
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run

interface Worker {
    suspend fun run(create: QubeEvent.CreatePod)
}

class WorkerImpl(val client: KubernetesClient, val podTemplate: Pod): Worker {
    private val threadPool = newFixedThreadPoolContext(4, "OrchestratorThreadPool")
    private val LOG by logger()

     private suspend fun createPod(pod: Pod) = run(threadPool) {
        client.createPod(pod)
        LOG.info("[{}] Pod created", pod.metadata.name)
    }

    private suspend fun deletePod(podName: String) = run(threadPool) {
        client.deletePod(podName)
        LOG.info("[{}] Pod deleted", podName)
    }

    private suspend fun runPod(podName: String, channel: Channel<Pod>, sendMessage: Channel<SentMessage>): Unit {
       for (message in channel) {
           val state = message.status.containerStatuses.firstOrNull()?.state

           if (state != null) {
               if (state.waiting != null) {
                   sendMessage.send(SentMessage.PodStatus(podName, Status.PENDING, null))
               }
               if (state.running != null) {
                   sendMessage.send(SentMessage.PodStatus(podName, Status.RUNNING, "$podName.qube"))
               }
               else if (state.terminated != null) {
                   if (state.terminated.exitCode == 0) {
                       sendMessage.send(SentMessage.PodStatus(podName, Status.SUCCESS, null))
                   }
                   else {
                       sendMessage.send(SentMessage.PodStatus(podName, Status.ERROR, null))
                   }
                   LOG.info("terminated {}", state)
                   return
               }
               else {
                   sendMessage.send(SentMessage.PodStatus(podName, Status.UNKNOWN, null))
               }
           }
       }
    }

    override suspend fun run(create: QubeEvent.CreatePod) {
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
                .editOrNewSpec()
                .editFirstContainer()
                .withImage(create.image)
                .withCommand(create.command)
                .endContainer()
                .endSpec()
                .build())

            runPod(podName, podEvents, create.returnChannel)
        }
        catch (e: Exception) {
           LOG.error("[{}] Exception while running pod", podName, e)
        }
        finally {
            deletePod(podName)
            watch.close()
        }
    }
}
