package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class Worker(val jobTemplate: Job, val queue: BlockingQueue<BildJob>, val client: DefaultKubernetesClient, val events: Observable<Event>): Runnable {
    val log by logger()
    val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    override fun run() {
        try {
            val job = queue.take()
            val jobName = "bild-${job.id.id}"
            log.info("Starting job: {}", jobName)
            val jobRunner = JobRunner(jobTemplate, jobName, client)
                val subscription = events.subscribeOn(scheduler)
                    .filter { event -> event.involvedObject.name == jobName }
                    .subscribe(jobRunner)
            try {
                jobRunner.run()
            }
            catch (e: Exception) {
                subscription.unsubscribe()
                throw e
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            log.error("exception", e)
        }
    }
}
