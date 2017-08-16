package io.quartic.bild.qube

import com.google.common.base.Stopwatch
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.client.Watch
import io.quartic.bild.model.CreationState
import io.quartic.bild.model.JobResult
import io.quartic.common.logging.logger
import rx.Subscriber
import java.util.concurrent.TimeUnit

class JobStateManager(
    private val job: Job,
    private val jobName: String,
    private val client: Qube,
    private val maxFailures: Int,
    private val creationTimeoutSeconds: Int,
    private val runTimeoutSeconds: Int,
    private val stopwatch: Stopwatch = Stopwatch.createUnstarted()
) {
    val log by logger()

    private var creationState: CreationState = CreationState.UNKNOWN
    private var watcher: Watch? = null
    // Record whether we've ever seen the job returned by the API
    private var jobSeen: Boolean = false

    sealed class JobState {
        data class Pending(val _dummy: Int = 0): JobState()
        data class Running(val _dummy: Int = 0): JobState()
        data class Failed(val jobResult: JobResult): JobState()
        data class Succeeded(val jobResult: JobResult): JobState()
    }

    fun cleanup() {
        log.info("[{}] Deleting job", jobName)
        client.deleteJob(job)
        watcher?.close()
    }

    fun subscriber(): Subscriber<Event> {
        return object: Subscriber<Event>() {
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
        }
    }

    fun getLogs(): Map<String, String?> {
        val job = client.getJob(jobName)
        if (job?.spec?.selector != null) {
            val pods = client.listPodsForJob(job)
            return pods
                .map { pod ->
                    val podName = pod.metadata.name
                    var logs: String? = null
                    try {
                        logs = client.getLogs(podName)
                    } catch (e: Exception) {
                        log.warn("[{}] Exception while fetching logs for pod: {}", jobName, podName)
                    }
                    podName to logs
                }
                .groupBy({ p -> p.first })
                .mapValues { v -> v.value.first().second }
        }
        return mapOf()
    }

    fun start() {
        log.info("[{}] Creating job", jobName)
        stopwatch.start()
        client.createJob(job)
    }

    fun poll(): JobState {
        val job = client.getJob(jobName)

        if (job == null) {
            if (jobSeen) {
                return failure("Job was deleted from underneath us.")
            } else {
                return failure("Job not returned when querying API for the first time.")
            }
        }

        jobSeen = true

        if (isSuccess(job)) {
            return success("Success")
        } else if (isFailure(job)) {
            return failure("Too many failures")
        }

        if (creationState == CreationState.FAILED) {
            return failure("Creation failed")
        }

        if (creationState == CreationState.UNKNOWN &&
            stopwatch.elapsed(TimeUnit.SECONDS) > creationTimeoutSeconds) {
            return failure("Exceeded creation timeout")
        }
        if (creationState == CreationState.CREATED &&
            stopwatch.elapsed(TimeUnit.SECONDS) > runTimeoutSeconds) {
            return failure("Exceeded run timeout")
        }

        if (creationState == CreationState.CREATED) {
            return JobState.Running()
        } else {
            return JobState.Pending()
        }
    }

    fun isSuccess(job: Job?) = job?.status?.succeeded != null && job.status.succeeded >= 1
    fun isFailure(job: Job?) = job?.status?.failed != null && job.status.failed >= maxFailures

    fun success(reason: String) = JobState.Succeeded(JobResult(true, getLogs(), reason))
    fun failure(reason: String) = JobState.Failed(JobResult(false, getLogs(), reason))

    companion object {
        val FAILED_CREATE = "FailedCreate"
        val SUCCESSFUL_CREATE = "SuccessfulCreate"
    }
}
