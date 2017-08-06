package io.quartic.bild

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.model.CustomerId
import io.quartic.bild.qube.JobLoop
import io.quartic.bild.qube.JobStateManager
import io.quartic.bild.qube.Qube
import io.quartic.bild.qube.Worker
import org.junit.Test
import rx.subjects.PublishSubject
import java.util.concurrent.BlockingQueue
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import rx.observers.TestSubscriber

class WorkerShould {
    @Test
    fun start_job() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        worker.runJob(bildJob)
        verify(jobRunner).start()
    }

    @Test
    fun call_cleanup_methods() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        worker.runJob(bildJob)
        verify(jobRunner).cleanup()
        assertThat(subscriber.isUnsubscribed, equalTo(true))
    }

    @Test
    fun cleanup_on_exception() {
        val subscriber = TestSubscriber.create<Event>()
        whenever(jobRunner.subscriber()).thenReturn(subscriber)
        whenever(jobLoop.loop(any(), any())).thenThrow(KubernetesClientException("wat"))
        worker.runJob(bildJob)
        verify(jobRunner).cleanup()
        assertThat(subscriber.isUnsubscribed, equalTo(true))
    }


    val bildJob = BildJob(BildId("1"), CustomerId("1"), BildPhase.TEST)
    val queue = mock<BlockingQueue<BildJob>>()
    val client = mock<Qube>()
    val events = PublishSubject.create<Event>()
    val jobResultStore = mock<JobResultStore>()
    val job = JobBuilder().build()
    val jobRunner = mock<JobStateManager>()
    val jobLoop = mock<JobLoop>()

    val worker = Worker(
        KubernetesConfiguraration("wat", job, 4, 100, 100, 100, "%s", true),
        queue,
        client,
        events,
        jobResultStore,
        { jobRunner },
        jobLoop
    )

}
