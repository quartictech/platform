package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.quartic.bild.JobResultStore
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class Worker(
    private val configuration: KubernetesConfiguraration,
    private val queue: BlockingQueue<BildJob>,
    private val client: Qube,
    private val events: Observable<Event>,
    private val jobResults: JobResultStore,
    private val jobStateManagerFactory: (job: BildJob) -> JobStateManager = { job ->
        createJobRunner(client, configuration, job)
    },
    private val jobLoop: JobLoop = JobLoop()
): Runnable {
    val log by logger()
    val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())!!

    fun eventObservable(job: BildJob) = events
        .filter { event -> event.involvedObject.name == jobName(job) }

    override fun run() {
        while (true) {
            val job = queue.take()
            runJob(job)
        }
    }

    fun runJob(job: BildJob) {
        try {
            val jobName = jobName(job)
            log.info("Starting job: {}", jobName(job))
            val jobRunner = jobStateManagerFactory(job)
            val subscription = eventObservable(job).subscribeOn(scheduler)
                .subscribe(jobRunner.subscriber())
            try {
                jobRunner.start()
                val result = jobLoop.loop(jobName, jobRunner)
                log.info("[{}] Job completed with result: {}", jobName, result)
                jobResults.putJobResult(job, result)
            }
            catch (e: Exception) {
                log.error("Exception while running job", e)
            }
            finally {
                jobRunner.cleanup()
                subscription.unsubscribe()
            }
        }
        catch (e: Exception) {
            log.error("Unexpected exception", e)
        }
    }

    companion object {
        fun createJobRunner(client: Qube, configuration: KubernetesConfiguraration, job: BildJob) = JobStateManager(
                createJob(configuration, job),
                jobName(job),
                client,
                configuration.maxFailures,
                configuration.creationTimeoutSeconds,
                configuration.runTimeoutSeconds
        )

        fun createJob(configuration: KubernetesConfiguraration, job: BildJob) = JobBuilder(configuration.template)
            .withNewMetadata()
            .withName(jobName(job))
            .withNamespace(configuration.namespace)
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


        fun jobName(job: BildJob) = "bild-${job.id.id}"
    }
}
