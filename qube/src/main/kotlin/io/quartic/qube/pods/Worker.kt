package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.quartic.common.logging.logger
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.store.JobStore
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

interface Worker {
    fun runAsync(create: QubeEvent.CreatePod): Job
}
class WorkerImpl(
    val client: KubernetesClient,
    val podTemplate: Pod,
    val namespace: String,
    val jobStore: JobStore,
    val timeoutSeconds: Long
): Worker {
    private val threadPool = newFixedThreadPoolContext(4, "Worker-Thread-Pool")
    private val LOG by logger()

    fun podName(key: PodKey) = "${key.client}-${key.name}"
    private suspend fun createPodAsync(pod: Pod) = async(threadPool) {
        client.createPod(pod)
        LOG.info("[{}] Pod created", pod.metadata.name)
    }

    private suspend fun deletePodAsync(podName: String) = async(threadPool) {
        client.deletePod(podName)
        LOG.info("[{}] Pod deleted", podName)
    }

    private suspend fun runPod(key: PodKey, channel: Channel<Pod>,
                               responses: Channel<QubeResponse>) =
        withTimeout(timeoutSeconds, TimeUnit.SECONDS) {
            val podName = podName(key)
            for (message in channel) {
                val state = message.status.containerStatuses.firstOrNull()?.state

                when {
                    state?.waiting != null -> {
                        LOG.info("[{}] Pod waiting", podName)
                        responses.send(QubeResponse.Waiting(key.name))
                    }
                    state?.running != null -> {
                        responses.send(QubeResponse.Running(key.name, "$podName.$namespace"))
                    }
                    state?.terminated != null -> {
                        if (state.terminated.exitCode == 0) {
                            responses.send(QubeResponse.Succeeded(key.name))
                        } else {
                            responses.send(QubeResponse.Failed(key.name, state.terminated.message))
                        }
                        LOG.info("[{}] terminated {}", podName, state)
                        return@withTimeout
                    }
                }
            }
        }

    private suspend fun run(create: QubeEvent.CreatePod) {
        val podName = podName(create.key)
        val watch = client.watchPod(podName)

        try {
            val startTime = Instant.now()
            createPodAsync(PodBuilder(podTemplate)
                .editOrNewMetadata()
                .withName(podName)
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .editFirstContainer()
                .withImage(create.image)
                .withCommand(create.command)
                .endContainer()
                .endSpec()
                .build())
                .await()

            runPod(create.key, watch.channel, create.returnChannel)
            val endTime = Instant.now()
            storeResult(podName, create, startTime, endTime)
                .await()
        }
        catch (e: Exception) {
            create.returnChannel.send(QubeResponse.Exception(create.key.name))
            LOG.error("[{}] Exception while running pod", podName, e)
        }
        finally {
            watch.close()
            withTimeout(10, TimeUnit.SECONDS) {
                deletePodAsync(podName).await()
            }
        }
    }

    override fun runAsync(create: QubeEvent.CreatePod) = async(CommonPool) { run(create) }


    private suspend fun storeResult(podName: String, create: QubeEvent.CreatePod,
                                    startTime: Instant, endTime: Instant) = async(threadPool) {
        try {
            val pod = client.getPod(podName)
            val logs = client.getLogs(podName)

            val terminatedState = pod.status.containerStatuses.firstOrNull()?.state?.terminated

            jobStore.insertJob(
                id = UUID.randomUUID(),
                client = create.key.client,
                podName = create.key.name,
                createPod = QubeRequest.Create(create.key.name, create.image, create.command),
                log = logs,
                startTime = startTime,
                endTime = endTime,
                reason = terminatedState?.reason,
                message = terminatedState?.message,
                exitCode = terminatedState?.exitCode
            )
        } catch (e: Exception) {
            LOG.error("[{}] Exception storing to postgres", podName, e)
        }
    }

}
