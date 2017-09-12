package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.*
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.qube.api.model.ContainerState
import io.quartic.qube.Database
import io.quartic.qube.api.model.ContainerSpec
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.Job
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
    val database: Database,
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
                val states = message.status.containerStatuses.map { it.state }

                val allReady = message.status.containerStatuses.all { it.ready ?: false }
                val anyTerminated = states.any { state -> state.terminated != null }
                val anyWaiting = states.any { state -> state.waiting != null }
                val allRunning = states.all { state -> state.running != null }

                when {
                    anyTerminated -> {
                        if (states.all { state -> state?.terminated == null || state.terminated?.exitCode == 0 }) {
                            responses.send(Terminated.Succeeded(key.name, ALL_CONTAINERS_SUCCEEDED_OR_DIDNT_TERMINATE))
                        } else {
                            responses.send(Terminated.Failed(key.name, SOME_CONTAINERS_FAILED))
                        }
                        LOG.info("[{}] terminated {}", podName, states)
                        return@withTimeout
                    }
                    anyWaiting -> {
                        LOG.info("[{}] Pod waiting", podName)
                        responses.send(QubeResponse.Waiting(key.name))
                    }
                    allRunning && allReady && message.status.podIP != null -> {
                        responses.send(QubeResponse.Running(key.name, message.status.podIP, podId))
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
            podBuilder.addToContainers(buildContainer(container))
        }

        return podBuilder.endSpec().build()
    }

    private fun buildContainer(container: ContainerSpec): Container {
         val builder = ContainerBuilder().withName(container.name)
             .withImage(container.image)

        if (container.command != null) {
            builder.withCommand(container.command)
        }

        builder.withEnv(container.env.map { entry ->
            EnvVar(entry.key, entry.value, null)
        })

        return builder.addNewPort()
            .withContainerPort(container.port)
            .endPort()

            .editOrNewReadinessProbe()
            .withNewTcpSocket()
            .withPort(IntOrString(container.port))
            .endTcpSocket()
            .withInitialDelaySeconds(3)
            .withPeriodSeconds(3)
            .endReadinessProbe()
            .build()
    }



    override fun runAsync(create: QubeEvent.CreatePod) = async(CommonPool) { run(create) }


    private suspend fun storeResult(podId: UUID, podName: String, create: QubeEvent.CreatePod,
                                    startTime: Instant, endTime: Instant) = run(threadPool) {
        try {
            val pod = client.getPod(podName)

            database.insertJob(
                id = podId,
                client = create.key.client,
                podName = create.key.name,
                createPod = QubeRequest.Create(create.key.name, create.pod),
                startTime = startTime,
                endTime = endTime,
                containers = pod.spec.containers.zip(pod.status.containerStatuses).associateTo(mutableMapOf()) {
                    (container, status) -> container.name to ContainerState(
                    status.state?.terminated?.exitCode,
                    status.state?.terminated?.reason,
                    status.state?.terminated?.message,
                    client.getLogs(podName, container.name))
                }
            )
        } catch (e: Exception) {
            LOG.error("[{}] Exception storing to postgres", podName, e)
        }
    }

    companion object {
        const val ALL_CONTAINERS_SUCCEEDED_OR_DIDNT_TERMINATE = "All containers either finished with success or didn't terminate"
        const val SOME_CONTAINERS_FAILED = "Some containers terminated with non-zero exit status"
    }

}
