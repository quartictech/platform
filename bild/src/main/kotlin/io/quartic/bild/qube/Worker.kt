package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.JobBuilder
import io.quartic.bild.JobResultStore
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import io.quartic.github.GithubInstallationClient
import org.slf4j.LoggerFactory
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
    github: GithubInstallationClient,
    private val jobStateManagerFactory: (job: BildJob) -> JobStateManager = { job: BildJob -> createJobRunner(client, configuration, job, github) },
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
        val log = LoggerFactory.getLogger(this::class.java)
        fun createJobRunner(client: Qube, configuration: KubernetesConfiguraration, job: BildJob, github: GithubInstallationClient) = JobStateManager(
                createJob(configuration, job, github),
                jobName(job),
                client,
                configuration.maxFailures,
                configuration.creationTimeoutSeconds,
                configuration.runTimeoutSeconds
        )

        fun createJob(configuration: KubernetesConfiguraration, job: BildJob, github: GithubInstallationClient): Job {
            val env = mutableListOf(
                EnvVar("QUARTIC_PHASE", job.phase.toString(), null),
                EnvVar("QUARTIC_JOB_ID", job.id.id, null),
                EnvVar("QUARTIC_BACKCHANNEL_ENDPOINT", String.format(configuration.backChannelEndpoint, job.id.id), null)
            )

            log.info("job: {}", job)

            if (job.installationId != null && job.cloneUrl != null) {
                val token = github.accessToken(job.installationId).token
                env.add(EnvVar("QUARTIC_REPO", job.cloneUrl.replace("https://", "https://x-access-token:$token@"), null))
                env.add(EnvVar("QUARTIC_COMMIT_REF", job.commit, null))
            }

            return JobBuilder(configuration.template)
                .withNewMetadata()
                .withName(jobName(job))
                .withNamespace(configuration.namespace)
                .endMetadata()
                .editSpec().editTemplate().editSpec().editFirstContainer()
                .addAllToEnv(env)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()
        }

        fun jobName(job: BildJob) = "bild-${job.id.id}"
    }
}
