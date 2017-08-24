package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.qube.api.model.ContainerSpec
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.PodKey
import io.quartic.qube.pods.QubeEvent
import io.quartic.qube.pods.WorkerImpl
import io.quartic.qube.store.JobStore
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import org.hamcrest.CoreMatchers
import java.util.*

class WorkerShould {
    val client = mock<KubernetesClient>()
    val podTemplate = PodBuilder()
        .editOrNewSpec()
        .addNewContainer()
        .endContainer()
        .endSpec()
        .build()

    val jobStore = mock<JobStore>()
    val worker = WorkerImpl(client, podTemplate, "noob", jobStore, 10, true)

    val key = PodKey(UUID.randomUUID(), "test")
    val pod = PodBuilder(podTemplate)
        .editOrNewMetadata()
        .withName("${key.client}-${key.name}")
        .withNamespace("noob")
        .endMetadata()
        .editOrNewSpec()
        .withHostname(key.name)
        .withSubdomain(key.client.toString())
        .editFirstContainer()
        .withImage("la-dispute-discography-docker:1")
        .withCommand(listOf("great music"))
        .addNewPort()
        .withContainerPort(8000)
        .endPort()
        .editOrNewReadinessProbe()
        .withNewTcpSocket()
        .withPort(IntOrString(8000))
        .endTcpSocket()
        .withInitialDelaySeconds(3)
        .withPeriodSeconds(3)
        .endReadinessProbe()

        .endContainer()
        .endSpec()
        .build()
    val returnChannel = mock<Channel<QubeResponse>>()
    val podEvents = Channel<Pod>(UNLIMITED)
    val containerSpec = ContainerSpec(
        "la-dispute-discography-docker:1",
        listOf("great music"),
        8000)

    init {
        whenever(client.watchPod(any()))
            .thenReturn(KubernetesClient.PodWatch(podEvents, mock()))
    }

    @Test
    fun watch_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))

            verify(client, timeout(1000)).watchPod(eq("${key.client}-${key.name}"))
        }
    }

    @Test
    fun create_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))

            verify(client, timeout(1000)).createPod(eq(pod))
        }
    }

    @Test
    fun send_status_on_pod_running_and_ready() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(runningPod(true))

            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Running(key.name, "100.100.100.100")
            )
        }
    }

    @Test
    fun not_send_status_on_pod_running_not_ready() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(runningPod(false))

            verify(returnChannel, timeout(1000).times(0)).send(
                QubeResponse.Running(key.name, "${key.name}.${key.client}.noob")
            )
        }
    }


    @Test
    fun send_status_on_pod_failed() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(podTerminated(1))

            verify(returnChannel, timeout(1000)).send(
                Terminated.Failed(key.name, "reason")
            )
        }
    }

     @Test
    fun send_status_on_pod_success() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(podTerminated(0))

            verify(returnChannel, timeout(1000)).send(
                Terminated.Succeeded(key.name, "reason")
            )
        }
    }

    @Test
    fun send_status_on_kube_failure() {
        whenever(client.createPod(any())).thenThrow(KubernetesClientException("Noob"))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(podTerminated(0))


            verify(returnChannel, timeout(1000)).send(
                Terminated.Exception(key.name, anyOrNull())
            )
        }
    }

    @Test
    fun store_to_postgres() {
        whenever(client.getPod(any())).thenReturn(podTerminated(0))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(podTerminated(0))

            verify(jobStore, timeout(1000)).insertJob(
                any(),
                eq(key.client),
                eq(key.name),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(0)
            )
        }
    }

    @Test
    fun handle_postgres_exception() {
        whenever(client.getPod(any())).thenReturn(podTerminated(0))
        whenever(jobStore.insertJob(
            anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(),
            anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenThrow(RuntimeException("noobhole"))
        runBlocking {
            val job = worker.runAsync(QubeEvent.CreatePod(key, returnChannel, containerSpec))
            podEvents.send(podTerminated(0))
            job.await()
            verify(returnChannel, times(0))
                .send(Terminated.Exception(key.name, any()))
        }
    }

    fun runningPod(ready: Boolean) = PodBuilder(pod).editOrNewStatus()
        .addNewContainerStatus()
        .withReady(ready)
        .editOrNewState()
        .editOrNewRunning()
        .endRunning()
        .endState()
        .endContainerStatus()
        .withPodIP("100.100.100.100")
        .endStatus()
        .build()

    fun podTerminated(exitCode: Int) = PodBuilder(pod).editOrNewStatus()
        .addNewContainerStatus()
        .editOrNewState()
        .editOrNewTerminated()
        .withExitCode(exitCode)
        .withMessage("noobout")
        .withReason("reason")
        .endTerminated()
        .endState()
        .endContainerStatus()
        .endStatus()
        .build()
}
