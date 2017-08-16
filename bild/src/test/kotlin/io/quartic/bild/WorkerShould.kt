package io.quartic.bild

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.quartic.bild.model.BuildId
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.model.JobResult
import io.quartic.bild.qube.JobLoop
import io.quartic.bild.qube.JobStateManager
import io.quartic.bild.qube.Qube
import io.quartic.bild.qube.Worker
import io.quartic.bild.store.BuildStore
import io.quartic.common.model.CustomerId
import io.quartic.github.GithubInstallationClient
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import java.util.concurrent.BlockingQueue

class WorkerShould {
    @Test
    fun start_job() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        worker.runJob(buildJob)
        verify(jobRunner).start()
    }

    @Test
    fun call_cleanup_methods() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        worker.runJob(buildJob)
        verify(jobRunner).cleanup()
        assertThat(subscriber.isUnsubscribed, equalTo(true))
    }

    @Test
    fun cleanup_on_exception() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        whenever(jobLoop.loop(any(), any())).thenThrow(KubernetesClientException("wat"))
        worker.runJob(buildJob)
        verify(jobRunner).cleanup()
        assertThat(subscriber.isUnsubscribed, equalTo(true))
    }

    @Test
    fun set_job_result() {
        val subscriber = TestSubscriber.create<Event>()
        val jobResult = JobResult(true, mapOf("noobPod" to "sweet"), "Nice job!")
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        whenever(jobLoop.loop(any(), any())).thenReturn(jobResult)
        worker.runJob(buildJob)
        verify(jobRunner).start()
        verify(buildStore).setJobResult(buildJob, jobResult)
    }

    val buildJob = BuildJob(BuildId("1"), CustomerId("1"), 213L, "http://wat", "wat", "hash", BuildPhase.TEST)
    val queue = mock<BlockingQueue<BuildJob>>()
    val client = mock<Qube>()
    val events = PublishSubject.create<Event>()
    val buildStore = mock<BuildStore>()
    val job = JobBuilder().build()
    val jobRunner = mock<JobStateManager>()
    val jobLoop = mock<JobLoop>()
    val github = mock<GithubInstallationClient>()

    val worker = Worker(
        KubernetesConfiguraration("wat", job, 4, 100, 100, 100, "%s", true),
        queue,
        client,
        events,
        buildStore,
        github,
        { jobRunner },
        jobLoop
    )

}
