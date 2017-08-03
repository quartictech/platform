package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.quartic.bild.JobResultStore
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.JobResult
import io.quartic.common.logging.logger
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class Worker(val configuration: KubernetesConfiguraration,
             val queue: BlockingQueue<BildJob>,
             val client: Qube,
             val events: Observable<Event>,
             val namespace: String,
             val jobResults: JobResultStore): Runnable {
    val log by logger()
    val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())!!

    fun jobName(job: BildJob) = "bild-${job.id.id}"

    fun makeJob(job: BildJob) = JobBuilder(configuration.template)
        .withNewMetadata()
        .withName(jobName(job))
        .withNamespace(namespace)
        .endMetadata()
        .editSpec().editTemplate().editSpec().editFirstContainer()
        .addAllToEnv(
            listOf(
                EnvVar("QUARTIC_PHASE", job.phase.toString(), null),
                EnvVar("QUARTIC_JOB_ID", job.id.toString(), null)
            ))
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build()

    fun eventObservable(job: BildJob) = events
        .filter { event -> event.involvedObject.name == jobName(job) }

    override fun run() {
        while (true) {
            val job = queue.take()
            runJob(job)
        }
    }

    fun jobLoop(jobName: String, jobRunner: JobRunner): JobResult {
        while (true) {
            val jobState = jobRunner.poll()
            when(jobState) {
                is JobRunner.JobState.Running -> log.info("[{}] Running", jobName)
                is JobRunner.JobState.Pending -> log.info("[{}] Pending", jobName)
                is JobRunner.JobState.Failed -> return jobState.jobResult
                is JobRunner.JobState.Succeeded -> return jobState.jobResult
            }

            log.info("[{}] Sleeping...", jobName)
            Thread.sleep(1000)
        }
    }

    fun runJob(job: BildJob) {
        try {
            val jobName = jobName(job)
            log.info("Starting job: {}", jobName(job))
            val jobRunner = JobRunner(
                makeJob(job),
                jobName,
                client,
                configuration.maxFailures,
                configuration.creationTimeoutSeconds,
                configuration.runTimeoutSeconds
                )
                val subscription = eventObservable(job).subscribeOn(scheduler)
                    .subscribe(jobRunner)
            try {
                jobRunner.start()
                val result = jobLoop(jobName, jobRunner)
                log.info("[{}] Job completed with result: {}", jobName, result)
                jobResults.putJobResult(job, result)
            }
            finally {
                jobRunner.cleanup()
                subscription.unsubscribe()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            log.error("Exception", e)
        }
    }
}
