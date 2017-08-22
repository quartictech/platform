package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.quartic.qube.api.Response
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.PodKey
import io.quartic.qube.pods.QubeEvent
import io.quartic.qube.pods.WorkerImpl
import io.quartic.qube.store.JobStore
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
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
    val returnChannel = mock<Channel<Response>>()
    val podEvents = Channel<Pod>()

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

            verify(client, timeout(1000)).createPod(eq(pod))
            verify(returnChannel, timeout(1000)).send(
                Response.PodRunning(key.name, "${key.client}-${key.name}.noob")
            )
        }
    }

     @Test
    fun sends_status_on_pod_terminated() {
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

            verify(client, timeout(1000)).createPod(eq(pod))
            verify(returnChannel, timeout(1000)).send(
                Response.PodRunning(key.name, "${key.client}-${key.name}.noob")
            )
        }
    }
}
