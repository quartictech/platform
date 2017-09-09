package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.ContainerBuilder
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.QubeResponse.Terminated
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
    val timeoutSeconds: Long,
    val deletePods: Boolean,
    val uuidGen: () -> UUID = { UUID.randomUUID() }
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

    private suspend fun runPod(podId: UUID, key: PodKey, channel: Channel<Pod>,
                               responses: Channel<QubeResponse>) =
        withTimeout(timeoutSeconds, TimeUnit.SECONDS) {
            val podName = podId.toString()
            for (message in channel) {
                val state = message.status.containerStatuses.firstOrNull()?.state
                val ready = message.status.containerStatuses.firstOrNull()?.ready

                when {
                    state?.waiting != null -> {
                        LOG.info("[{}] Pod waiting", podName)
                        responses.send(QubeResponse.Waiting(key.name))
                    }
                    state?.running != null && ready != null && ready && message.status.podIP != null -> {
                        responses.send(QubeResponse.Running(key.name, message.status.podIP, podId))
                    }
                    state?.terminated != null -> {
                        if (state.terminated.exitCode == 0) {
                            responses.send(Terminated.Succeeded(key.name, state.terminated.reason))
                        } else {
                            responses.send(Terminated.Failed(key.name, state.terminated.reason))
                        }
                        LOG.info("[{}] terminated {}", podName, state)
                        return@withTimeout
                    }
                }
            }
        }

    private suspend fun run(create: QubeEvent.CreatePod) {
        val podId = uuidGen()
        val podName = podId.toString()
        val watch = client.watchPod(podName)

        val startTime = Instant.now()
        cancellable(
            block = {
                createPod(pod(podName, create))

                runPod(podId, create.key, watch.channel, create.returnChannel)
            },
            onThrow = { throwable ->
                launch(NonCancellable) {
                    LOG.error("[{}] Exception while running pod", podName, throwable)
                    create.returnChannel.send(Terminated.Exception(create.key.name, "Exception while running pod"))
                }
            },
            onFinally = {
                launch(NonCancellable) {
                    watch.close()
                    withTimeout(10, TimeUnit.SECONDS) {
                        val endTime = Instant.now()
                        storeResult(podId, podName, create, startTime, endTime)
                    }

                    if (deletePods) {
                        withTimeout(10, TimeUnit.SECONDS) {
                            deletePod(podName)
                        }
                    }
                }
            }
        )
    }

    private fun pod(podName: String, create: QubeEvent.CreatePod): Pod {
        val podBuilder = PodBuilder(podTemplate)
            .editOrNewMetadata()
            .withName(podName)
            .withNamespace(namespace)
            .endMetadata()
            .editOrNewSpec()
            .withContainers(listOf())

            create.pod.containers.forEach { container ->
                podBuilder
                    .addToContainers(
                        ContainerBuilder()
                            .withImage(container.image)
                            .withCommand(container.command)

                            .addNewPort()
                            .withContainerPort(container.port)
                            .endPort()

                            .editOrNewReadinessProbe()
                            .withNewTcpSocket()
                            .withPort(IntOrString(container.port))
                            .endTcpSocket()
                            .withInitialDelaySeconds(3)
                            .withPeriodSeconds(3)
                            .endReadinessProbe()
                            .build())
            }

        return podBuilder.endSpec().build()
    }



    override fun runAsync(create: QubeEvent.CreatePod) = async(CommonPool) { run(create) }


    private suspend fun storeResult(podId: UUID, podName: String, create: QubeEvent.CreatePod,
                                    startTime: Instant, endTime: Instant) = run(threadPool) {
        try {
            val pod = client.getPod(podName)
            val logs = client.getLogs(podName)

            val terminatedState = pod.status.containerStatuses.firstOrNull()?.state?.terminated

            jobStore.insertJob(
                id = podId,
                client = create.key.client,
                podName = create.key.name,
                createPod = QubeRequest.Create(create.key.name, create.pod),
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
