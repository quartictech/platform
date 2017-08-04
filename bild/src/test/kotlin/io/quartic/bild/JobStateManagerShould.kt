package io.quartic.bild

import com.google.common.base.Stopwatch
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.quartic.bild.qube.JobStateManager
import io.quartic.bild.qube.Qube
import org.junit.Test
import rx.subjects.PublishSubject
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JobStateManagerShould {
    private val job = JobBuilder().build()
    private val qube = mock<Qube>()
    private val subject = PublishSubject.create<Event>()
    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val stopwatch = mock<Stopwatch>()

    init {
        whenever(qube.createJob(job)).thenReturn(job)
        whenever(qube.getJob(JOB_NAME)).thenReturn(job)
    }

    @Test
    fun creates_the_job() {
        val jobRunner = jobRunner()
        jobRunner.start()
        verify(qube).createJob(job)
        assertThat<JobStateManager.JobState>(jobRunner.poll(), equalTo(JobStateManager.JobState.Pending()))
    }

    @Test
    fun detects_job_running() {
        val jobRunner = jobRunner()
        jobRunner.start()

        val event = mock<Event>()
        whenever(event.reason).thenReturn("SuccessfulCreate")
        subject.onNext(event)
        assertThat<JobStateManager.JobState>(jobRunner.poll(), equalTo(JobStateManager.JobState.Running()))
    }

    @Test
    fun fail_if_too_long_creating() {
        val jobRunner = jobRunner()
        jobRunner.start()
        whenever(stopwatch.elapsed(TimeUnit.SECONDS)).thenReturn(60000)
        val jobState = jobRunner.poll()
        assertThat<Class<JobStateManager.JobState>>(jobState.javaClass, equalTo(JobStateManager.JobState.Failed::class.java))
    }

    @Test
    fun handle_job_deletion() {
        val jobRunner = jobRunner()
        jobRunner.start()
        val resultA = jobRunner.poll()
        assertThat<JobStateManager.JobState>(resultA, equalTo(JobStateManager.JobState.Pending()))
        whenever(qube.getJob(any())).thenReturn(null)
        val resultB = jobRunner.poll()
        assertThat<Class<JobStateManager.JobState>>(resultB.javaClass, equalTo(JobStateManager.JobState.Failed::class.java))
    }

    fun jobRunner(): JobStateManager {
        val jobRunner = JobStateManager(
            job,
            JOB_NAME,
            qube,
            1,
            60,
            60,
            stopwatch)

        subject.subscribeOn(scheduler).subscribe(jobRunner.subscriber())
        return jobRunner
    }

    companion object {
        val JOB_NAME = "test"
    }

}
