package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import rx.subjects.PublishSubject
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class JobPool(val jobTemplate: Job, val client: DefaultKubernetesClient, val queue: BlockingQueue<BildJob>) {
    val log by logger()
    val namespace = NamespaceBuilder().editOrNewMetadata().withName(NAMESPACE).endMetadata().build()
    val threadPool = Executors.newFixedThreadPool(NUM_WORKERS);

    val events = PublishSubject.create<Event>()

    init {
        log.info("creating namespace")
        client.namespaces().createOrReplace(namespace)
        log.info("watching namespace")
        watchNamespace()

        for (i in 0..NUM_WORKERS - 1) {
            threadPool.submit(
                Worker(jobTemplate, queue, DefaultKubernetesClient(client.configuration), events)
            )
        }
    }

    fun watchNamespace() {
       client.inNamespace(NAMESPACE).events().watch(object: Watcher<Event> {
            override fun eventReceived(action: Watcher.Action?, resource: Event?) {
                events.onNext(resource)
            }

            override fun onClose(cause: KubernetesClientException?) {
                log.error("closed: {}", cause)
            }
        })
    }

    companion object {
        val NUM_WORKERS = 4
        val NAMESPACE = "bild"
    }
}
