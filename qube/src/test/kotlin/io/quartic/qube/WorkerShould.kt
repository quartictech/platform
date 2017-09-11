package io.quartic.qube

import com.nhaarman.mockito_kotlin.*
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.client.KubernetesClientException
import io.quartic.qube.api.QubeResponse
import io.quartic.qube.api.QubeResponse.Terminated
import io.quartic.qube.api.model.ContainerSpec
import io.quartic.qube.api.model.ContainerState
import io.quartic.qube.api.model.PodSpec
import io.quartic.qube.pods.*
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

    val uuid = UUID.randomUUID()
    val jobStore = mock<Database>()
    val worker = WorkerImpl(client, podTemplate, "noob", jobStore, 10, true, { -> uuid })

    val key = PodKey(UUID.randomUUID(), "test")
    val pod = PodBuilder(podTemplate)
        .editOrNewMetadata()
        .withName("$uuid")
        .withNamespace("noob")
        .endMetadata()
        .editOrNewSpec()
        .withContainers(container("leet-band", 8000), container("awesome-music", 8001))
        .endSpec()
        .build()
    val returnChannel = mock<Channel<QubeResponse>>()
    val podEvents = Channel<Pod>(UNLIMITED)
    val podSpec = PodSpec(listOf(
        ContainerSpec(
        "leet-band",
        "la-dispute-discography-docker:1",
        listOf("king-park"),
        8000),
        ContainerSpec(
        "awesome-music",
        "la-dispute-discography-docker:1",
        listOf("king-park"),
        8001)
    ))

    init {
        whenever(client.watchPod(any()))
            .thenReturn(KubernetesClient.PodWatch(podEvents, mock()))
    }

    @Test
    fun watch_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))

            verify(client, timeout(1000)).watchPod(eq("$uuid"))
        }
    }

    @Test
    fun create_pod() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))

            verify(client, timeout(1000)).createPod(eq(pod))
        }
    }

    @Test
    fun send_status_on_pod_running_and_ready() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(true, true),
                PodState(true, true))
            )

            verify(returnChannel, timeout(1000)).send(
                QubeResponse.Running(key.name, "100.100.100.100", uuid)
            )
        }
    }

    @Test
    fun not_send_status_on_pod_running_not_ready() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(true, false),
                PodState(true, true))
            )

            verify(returnChannel, timeout(1000).times(0)).send(
                isA<QubeResponse.Running>()
            )
        }
    }

    @Test
    fun send_status_on_pod_failed() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(false, false, 1),
                PodState(true, true)
            ))

            verify(returnChannel, timeout(1000)).send(
                Terminated.Failed(key.name, WorkerImpl.SOME_CONTAINERS_FAILED)
            )
        }
    }

    @Test
    fun send_status_on_pod_success() {
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(
                pods(
                    PodState(false, false, 0),
                    PodState(true, true)
                ))

            verify(returnChannel, timeout(1000)).send(
                Terminated.Succeeded(key.name, WorkerImpl.ALL_CONTAINERS_SUCCEEDED_OR_DIDNT_TERMINATE)
            )
        }
    }

    @Test
    fun send_status_on_kube_failure() {
        whenever(client.createPod(any())).thenThrow(KubernetesClientException("Noob"))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(false, false, 0),
                PodState(false, false, 0)
            ))

            verify(returnChannel, timeout(1000)).send(
                Terminated.Exception(key.name, anyOrNull())
            )
        }
    }

    @Test
    fun store_to_postgres_on_success() {
        whenever(client.getPod(any())).thenReturn(pods(
            PodState(false, false, 0),
            PodState(false, false, 0)
        ))
        runBlocking {
            worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(false, false, 0),
                PodState(false, false, 0)
            ))

            verify(jobStore, timeout(1000)).insertJob(
                any(),
                eq(key.client),
                eq(key.name),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        }
    }

    @Test
    fun store_to_postgres_on_cancel() {
        whenever(client.getPod(any())).thenReturn(pods(
            PodState(false, false, 0),
            PodState(false, false, 0)
        ))
        runBlocking {
            val job = worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            verify(client, timeout(1000)).createPod(eq(pod))

            job.cancel()

            verify(jobStore, timeout(1000)).insertJob(
                any(),
                eq(key.client),
                eq(key.name),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(mapOf(
                    "leet-band" to ContainerState(0, "reason", "noobout", null),
                    "awesome-music" to ContainerState(0, "reason", "noobout", null)
                ))
            )
        }
    }

    @Test
    fun handle_postgres_exception() {
        whenever(client.getPod(any())).thenReturn(pods(
            PodState(false, false, 0),
            PodState(false, false, 0)
        ))
        whenever(jobStore.insertJob(
            anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
        )).thenThrow(RuntimeException("noobhole"))
        runBlocking {
            val job = worker.runAsync(QubeEvent.CreatePod(key, returnChannel, podSpec))
            podEvents.send(pods(
                PodState(false, false, 0),
                PodState(false, false, 0)
            ))

            job.await()
            verify(returnChannel, times(0))
                .send(Terminated.Exception(key.name, any()))
        }
    }

    data class PodState(
        val running: Boolean,
        val ready: Boolean,
        val exitCode: Int? = null
    )

    fun pods(vararg podStates: PodState) = PodBuilder(pod).editOrNewStatus()
        .withContainerStatuses(podStates.map { podState ->
            if (podState.running) {
                ContainerStatusBuilder().withReady(podState.ready).editOrNewState()
                    .editOrNewRunning().endRunning().endState().build()
            } else {
                ContainerStatusBuilder().editOrNewState().editOrNewTerminated().withExitCode(podState.exitCode)
                    .withMessage("noobout")
                    .withReason("reason")
                    .endTerminated()
                    .endState().build()
            }
        })
        .withPodIP("100.100.100.100")
        .endStatus()
        .build()

//    fun podTerminated(exitCode: List<Int?>) = PodBuilder(pod).editOrNewStatus()
//        .withContainerStatuses(exitCode.map {
//            if (it != null) {
//                ContainerStatusBuilder().editOrNewState().editOrNewTerminated().withExitCode(it)
//                    .withMessage("noobout")
//                    .withReason("reason")
//                    .endTerminated()
//                    .endState().build()
//            } else {
//                ContainerStatusBuilder().withReady(true).editOrNewState().editOrNewRunning()
//                    .withReason("reason")
//                    .endTerminated()
//                    .endState().build()
//            }
//            })
//        .endStatus()
//        .build()

    fun container(name: String, port: Int) = ContainerBuilder()
        .withName(name)
        .withImage("la-dispute-discography-docker:1")
        .withCommand(listOf("king-park"))
        .addNewPort()
        .withContainerPort(port)
        .endPort()
        .editOrNewReadinessProbe()
        .withNewTcpSocket()
        .withPort(IntOrString(port))
        .endTcpSocket()
        .withInitialDelaySeconds(3)
        .withPeriodSeconds(3)
        .endReadinessProbe()
        .build()
}
