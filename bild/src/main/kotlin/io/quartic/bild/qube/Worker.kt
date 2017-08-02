package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.quartic.bild.JobResultStore
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class Worker(val configuration: KubernetesConfiguraration,
             val queue: BlockingQueue<BildJob>, val client: DefaultKubernetesClient,
             val events: Observable<Event>, val namespace: String,
             val jobResults: JobResultStore): Runnable {
    val log by logger()
    val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

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

    // This is needed because fabric8 isn't uptodate atm
    fun jobOps() = JobOperationsImpl(client.httpClient, client.configuration, "v1", namespace, null,
        true, null, null, false, -1, TreeMap<String, String>(), TreeMap<String, String>(), TreeMap<String, Array<String>>(),
        TreeMap<String, Array<String>>(),
        TreeMap<String, String>())

    override fun run() {
        try {
            val job = queue.take()
            val jobName = jobName(job)
            log.info("Starting job: {}", jobName(job))
            val jobRunner = JobRunner(
                makeJob(job),
                jobName,
                jobOps().inNamespace(namespace),
                client.inNamespace(namespace),
                configuration.maxFailures,
                configuration.creationTimeoutSeconds,
                configuration.runTimeoutSeconds
                )
                val subscription = events.subscribeOn(scheduler)
                    .filter { event -> event.involvedObject.name == jobName }
                    .subscribe(jobRunner)
            try {
                val result = jobRunner.run()
                jobResults.putJobResult(job, result)
            }
            finally {
                subscription.unsubscribe()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            log.error("exception", e)
        }
    }
}