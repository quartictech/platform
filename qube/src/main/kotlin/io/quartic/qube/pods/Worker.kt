package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.quartic.common.logging.logger
import io.quartic.qube.api.Request
import io.quartic.qube.api.Response
import io.quartic.qube.store.JobStore
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.time.Instant
import java.util.*

interface Worker {
    fun runAsync(create: QubeEvent.CreatePod): Job
}
class WorkerImpl(
    val client: KubernetesClient,
    val podTemplate: Pod,
    val namespace: String,
    val jobStore: JobStore
): Worker {
    private val threadPool = newFixedThreadPoolContext(4, "Worker-Thread-Pool")
    private val LOG by logger()

     private suspend fun createPod(pod: Pod) = run(threadPool) {
        client.createPod(pod)
        LOG.info("[{}] Pod created", pod.metadata.name)
    }

    private suspend fun deletePod(podName: String) = run(threadPool) {
        client.deletePod(podName)
        LOG.info("[{}] Pod deleted", podName)
    }

    private suspend fun runPod(podName: String, channel: Channel<Pod>,
                               responses: Channel<Response>) {
        for (message in channel) {
            val state = message.status.containerStatuses.firstOrNull()?.state

            if (state != null) {
                if (state.waiting != null) {
                    LOG.info("[{}] Pod waiting", podName)
                    responses.send(Response.PodWaiting(podName))
                }
                if (state.running != null) {
                    responses.send(Response.PodRunning(podName, "$podName.$namespace"))
                }
                else if (state.terminated != null) {
                    if (state.terminated.exitCode == 0) {
                        responses.send(Response.PodSucceeded(podName))
                    }
                    else {
                        responses.send(Response.PodFailed(podName))
                    }
                    LOG.info("terminated {}", state)
                    return
                }
            }
        }
    }

    override fun runAsync(create: QubeEvent.CreatePod): Job = async(CommonPool) {
        val podName = "${create.key.client}-${create.key.name}"
        val podEvents = Channel<Pod>()
        val watch = client.watchPod(podName) { _, pod ->
            if (pod != null) {
                podEvents.offer(pod)
            }
        }

        try {
            val startTime = Instant.now()
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
            val endTime = Instant.now()
            storeResult(podName, create, startTime, endTime)
        }
        catch (e: Exception) {
           LOG.error("[{}] Exception while running pod", podName, e)
        }
        finally {
            deletePod(podName)
            watch.close()
        }
    }

    private suspend fun storeResult(podName: String, create: QubeEvent.CreatePod, startTime: Instant, endTime: Instant) {
        run(threadPool) {
            val pod = client.getPod(podName)
            val logs = client.getLogs(podName)

            val terminatedState = pod.status.containerStatuses.firstOrNull()?.state?.terminated

            jobStore.insertJob(
                id = UUID.randomUUID(),
                client = create.key.client,
                podName = create.key.name,
                createPod = Request.CreatePod(create.key.name, create.image, create.command),
                log = logs,
                startTime = startTime,
                endTime = endTime,
                reason = terminatedState?.reason,
                message = terminatedState?.message,
                exitCode = terminatedState?.exitCode
            )
        }

    }
}
