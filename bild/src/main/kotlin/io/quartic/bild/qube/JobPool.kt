package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.quartic.bild.KubernetesConfiguraration
import io.quartic.bild.model.BuildJob
import io.quartic.bild.store.JobStore
import io.quartic.common.logging.logger
import rx.subjects.PublishSubject
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import io.quartic.github.GithubInstallationClient

class JobPool(configuration: KubernetesConfiguraration,
              private val client: Qube,
              queue: BlockingQueue<BuildJob>,
              jobResults: JobStore,
              githubClient: GithubInstallationClient) {
    private val log by logger()
    private val namespace = NamespaceBuilder()
        .editOrNewMetadata()
        .withName(configuration.namespace)
        .endMetadata()
        .build()
    private val threadPool = Executors.newFixedThreadPool(configuration.numConcurrentJobs)
    private val events = PublishSubject.create<Event>()

    init {
        log.info("Creating namespace: $namespace")
        client.ensureNamespaceExists(namespace)
        watchNamespace()

        for (i in 0..configuration.numConcurrentJobs - 1) {
            threadPool.submit(
                Worker(
                    configuration,
                    queue,
                    client,
                    events,
                    jobResults,
                    githubClient
                )
            )
        }
    }

    private fun watchNamespace() {
        client.watchEvents { event -> events.onNext(event) }
    }
}
