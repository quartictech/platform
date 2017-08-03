package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.quartic.bild.qube.JobRunner
import io.quartic.bild.qube.Qube
import org.junit.Test
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import rx.schedulers.Schedulers
import java.util.concurrent.Executors

class JobRunnerShould {
    private val job = JobBuilder().build()
    private val qube = mock<Qube>()
    private val subject = PublishSubject.create<Event>()
    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    init {
        whenever(qube.createJob(job)).thenReturn(job)
        whenever(qube.getJob(JOB_NAME)).thenReturn(job)
    }

    @Test
    fun creates_the_job() {
        val jobRunner = jobRunner()
        jobRunner.start()
        verify(qube).createJob(job)
        assertThat<JobRunner.JobState>(jobRunner.poll(), equalTo(JobRunner.JobState.Pending()))
    }

    @Test
    fun detects_job_running() {
        val jobRunner = jobRunner()
        jobRunner.start()

        val event = mock<Event>()
        whenever(event.reason).thenReturn("SuccessfulCreate")
        subject.onNext(event)
        assertThat<JobRunner.JobState>(jobRunner.poll(), equalTo(JobRunner.JobState.Running()))
    }

    fun jobRunner(): JobRunner {
        val jobRunner = JobRunner(
            job,
            JOB_NAME,
            qube,
            1,
            60,
            60)

        subject.subscribeOn(scheduler).subscribe(jobRunner)
        return jobRunner
    }

    companion object {
        val JOB_NAME = "test"
    }

}
