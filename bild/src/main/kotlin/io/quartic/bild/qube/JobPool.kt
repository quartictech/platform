package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.quartic.bild.JobResultStore
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BildJob
import io.quartic.common.logging.logger
import rx.subjects.PublishSubject
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors

class JobPool(val configuration: KubernetesConfiguraration, val client: DefaultKubernetesClient,
              queue: BlockingQueue<BildJob>, jobResults: JobResultStore) {
    val log by logger()
    val namespace = NamespaceBuilder()
        .editOrNewMetadata()
        .withName(configuration.namespace)
        .endMetadata()
        .build()
    val threadPool = Executors.newFixedThreadPool(configuration.numConcurrentJobs);
    val events = PublishSubject.create<Event>()

    init {
        log.info("Creating namespace: $namespace")
        client.namespaces().createOrReplace(namespace)
        watchNamespace()

        for (i in 0..configuration.numConcurrentJobs - 1) {
            threadPool.submit(
                Worker(
                    configuration,
                    queue,
                    DefaultKubernetesClient(client.configuration),
                    events,
                    configuration.namespace,
                    jobResults
                )
            )
        }
    }

    fun watchNamespace() {
       client.inNamespace(configuration.namespace).events().watch(object: Watcher<Event> {
            override fun eventReceived(action: Watcher.Action?, resource: Event?) {
                events.onNext(resource)
            }

            override fun onClose(cause: KubernetesClientException?) {
                log.error("closed: {}", cause)
            }
        })
    }
}
