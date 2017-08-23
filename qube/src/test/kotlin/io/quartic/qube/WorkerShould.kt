package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.PodKey
import io.quartic.qube.pods.QubeEvent
import io.quartic.qube.pods.WorkerImpl
import io.quartic.qube.store.JobStore
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
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
    val worker = WorkerImpl(client, podTemplate, "noob", jobStore, 10)

    val key = PodKey(UUID.randomUUID(), "test")
    val pod = PodBuilder(podTemplate)
        .editOrNewMetadata()
        .withName("${key.client}-${key.name}")
        .withNamespace("noob")
        .endMetadata()
        .editOrNewSpec()
        .editFirstContainer()
        .withImage("la-dispute-discography-docker:1")
        .withCommand(listOf("great music"))
        .endContainer()
        .endSpec()
        .build()
    val returnChannel = mock<Channel<QubeResponse>>()
    val podEvents = Channel<Pod>(UNLIMITED)

    init {
        whenever(client.watchPod(any()))
            .thenReturn(KubernetesClient.PodWatch(podEvents, mock()))
    }

    @Test
    fun watches_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))

            verify(client, timeout(1000)).watchPod(eq("${key.client}-${key.name}"))
        }
    }

    @Test
    fun creates_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))

            verify(client, timeout(1000)).createPod(eq(pod))
        }
    }

    @Test
    fun sends_status_on_pod_running() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
            podEvents.send(
                PodBuilder(pod).editOrNewStatus()
                    .addNewContainerStatus()
                    .editOrNewState()
                    .editOrNewRunning()
                    .endRunning()
                    .endState()
                    .endContainerStatus()
                    .endStatus()
                    .build()
            )

            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Running(key.name, "${key.client}-${key.name}.noob")
            )
        }
    }

    @Test
    fun sends_status_on_pod_failed() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
            podEvents.send(podTerminated(1))

            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Failed(key.name, "noobout")
            )
        }
    }

     @Test
    fun sends_status_on_pod_success() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
            podEvents.send(podTerminated(0))

            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Succeeded(key.name)
            )
        }
    }

    @Test
    fun send_status_on_kube_failure() {
        whenever(client.createPod(any())).thenThrow(KubernetesClientException("Noob"))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
            podEvents.send(podTerminated(0))


            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Exception(key.name)
            )
        }
    }

    @Test
    fun store_to_postgres() {
        whenever(client.getPod(any())).thenReturn(podTerminated(0))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
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
            val job = worker.runAsync(QubeEvent.CreatePod(key, returnChannel,
                "la-dispute-discography-docker:1",
                listOf("great music")))
            podEvents.send(podTerminated(0))
            job.await()
            verify(returnChannel, times(0))
                .send(QubeResponse.Exception(key.name))
        }
    }

    fun podTerminated(exitCode: Int) = PodBuilder(pod).editOrNewStatus()
        .addNewContainerStatus()
        .editOrNewState()
        .editOrNewTerminated()
        .withExitCode(exitCode)
        .withMessage("noobout")
        .endTerminated()
        .endState()
        .endContainerStatus()
        .endStatus()
        .build()
}
