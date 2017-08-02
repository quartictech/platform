package io.quartic.bild.qube

import com.google.common.base.Stopwatch
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.client.NamespacedKubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation
import io.fabric8.kubernetes.client.dsl.ScalableResource
import io.quartic.bild.model.CreationState
import io.quartic.bild.model.JobResult
import io.quartic.common.logging.logger
import rx.Subscriber
import java.util.concurrent.TimeUnit


class JobRunner(
    val job: Job,
    val jobName: String,
    val jobOps: NonNamespaceOperation<Job, JobList, DoneableJob, ScalableResource<Job, DoneableJob>>,
    val client: NamespacedKubernetesClient,
    val maxFailures: Int,
    val creationTimeoutSeconds: Int,
    val runTimeoutSeconds: Int) : Subscriber<Event>() {
    val log by logger()

    private var creationState: CreationState = CreationState.UNKNOWN
    private var watcher: Watch? = null

    override fun onNext(event: Event?) {
        log.info("[{}] Received event: {}\n\t{}", jobName, event?.reason, event?.message)
        if (event?.reason == FAILED_CREATE) {
            creationState = CreationState.FAILED
        } else if (event?.reason == SUCCESSFUL_CREATE) {
            creationState = CreationState.CREATED
        }
    }

    override fun onCompleted() {
        log.error("Observable has completed. This should not happen.")
    }

    override fun onError(e: Throwable?) {
        log.error("Observable has errored. This should not happen.", e)
    }

    fun cleanup() {
        log.info("[{}] Deleting job", jobName)
        jobOps.delete(job)
        watcher?.close()
    }

    fun getLogs(): Map<String, String> {
        val job = jobOps.withName(jobName).get()
        if (job?.spec?.selector != null) {
            val pods = client.pods().withLabelSelector(job.spec.selector).list().items
            return pods.map { pod ->
                val podName = pod.metadata.name
                val logs = client.pods().withName(podName).getLog(true)
                podName to logs
            }.groupBy({ p -> p.first })
                .mapValues { v -> v.value.first().second }
        }
        return mapOf()
    }

    fun innerRun(): Job {
        log.info("[{}] Creating job", jobName)
        jobOps.create(job)
        val stopwatch = Stopwatch.createStarted()

        while (true) {
            val job = jobOps.withName(jobName).get()

            if (job.status.succeeded != null && job.status.succeeded >= 1) {
                log.info("[{}] Completion due to success", jobName)
                return job
            }

            if (job.status.failed != null && job.status.failed >= maxFailures) {
                log.info("[{}] Too many failures", jobName)
                return job
            }

            if (creationState == CreationState.FAILED) {
                log.info("[{}] Creation failed", jobName)
                return job
            }

            if (creationState == CreationState.UNKNOWN &&
                stopwatch.elapsed(TimeUnit.SECONDS) > creationTimeoutSeconds) {
                log.info("[{}] Exceeded creation timeout", jobName)
                return job
            }
            if (creationState == CreationState.CREATED &&
                stopwatch.elapsed(TimeUnit.SECONDS) > runTimeoutSeconds) {
                log.info("[{}] Exceeded run timeout", jobName)
                return job
            }

            log.info("[{}] Sleeping...", jobName)
            Thread.sleep(1000)
        }
    }

    fun isSuccess(job: Job?): Boolean {
        if (job != null) {
           return job.status.succeeded >= 1
        }
        return false
    }

    fun run(): JobResult {
        var job: Job? = null
        try {
            job = innerRun()
        } catch (e: Exception) {
            log.error("Exception while running job", e)
        }
        val result = JobResult(creationState, isSuccess(job), getLogs())
        log.info("[{}] Job completed with result: {}", jobName, result)
        cleanup()
        return result
    }

    companion object {
        val FAILED_CREATE = "FailedCreate"
        val SUCCESSFUL_CREATE = "SuccessfulCreate"
    }
}
