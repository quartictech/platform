package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.quartic.common.logging.logger
import rx.Subscriber
import java.util.TreeMap

class JobRunner(jobTemplate: Job, val jobName: String, val client: DefaultKubernetesClient) : Subscriber<Event>() {
    val log by logger()

    enum class State {
        UNKNOWN,
        FAILED,
        PENDING,
        RUNNING,
        ERROR,
        COMPLETE
    }

    private var state: State = State.UNKNOWN
    private var podName: String? = null
    private var watcher: Watch? = null

    fun mutateState(newState: State) {
        synchronized(state, {
            state = newState
        })
    }

    fun setPodName(val podName: String) {
        synchronized(podName, { this.podName = podName })
    }

    override fun onNext(event: Event?) {
        log.info("[{}] received event: {}\n\t{}", jobName, event?.reason, event?.message)
        if (event?.reason == "FailedCreate") {
            mutateState(State.FAILED)
        }
        else if (event?.reason == "SuccessfulCreate") {
            mutateState(State.PENDING)
        }
    }

    override fun onCompleted() {
        log.error("completed")
    }

    override fun onError(e: Throwable?) {
        log.error("error", e)
    }

    // This is needed because fabric8 isn't uptodate atm
    fun jobs() = JobOperationsImpl(client.httpClient, client.configuration, "v1", "bild", null,
        true, null, null, false, -1, TreeMap<String, String>(), TreeMap<String, String>(), TreeMap<String, Array<String>>(),
        TreeMap<String, Array<String>>(),
        TreeMap<String, String>())

     val job = JobBuilder(jobTemplate).withNewMetadata()
            .withNamespace(JobPool.NAMESPACE)
            .withName(jobName)
            .endMetadata()
            .build()

    fun watchJob(job: Job): Watch {
        return jobs().withName(jobName)
            .watch(object: Watcher<Job> {
                override fun eventReceived(action: Watcher.Action?, resource: Job?) {
                    log.info("[{}] {}", job)
                }

                override fun onClose(cause: KubernetesClientException?) {
                    log.info("[{}] closed job watch")
                }
            })
    }

    fun watchPodLog(job: Job): Watch? {
       return client.pods().withLabelSelector(job.spec.selector).watch(object: Watcher<Pod> {
           override fun onClose(cause: KubernetesClientException?) {
               log.info("[{}] closed pod watch")
           }

           override fun eventReceived(action: Watcher.Action?, resource: Pod?) {
               log.info("received pod event: {}", resource)
           }

       })
    }

    fun cleanup() {
        log.info("[{}] deleting job", jobName)
        jobs().delete(job)
        watcher?.close()
    }

    fun innerRun() {
        log.info("[{}] creating job", jobName)
        val job = jobs().create(job)

        while (true) {
            val job = jobs().withName(jobName).get()

            if (job.status.succeeded == 1) {
                mutateState(State.COMPLETE)
            }

            if (state == State.FAILED || state == State.COMPLETE) {
                return
            }

            log.info("[{}] Sleeping...", jobName)
            Thread.sleep(1000)
        }
    }

    fun run(): State {
        try {
            innerRun()
        }
        catch (e: Exception) {
            log.error("exception while running job")
        }
        cleanup()
        log.info("job finished with state: {}", state)
        return state
    }


}
