package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.quartic.common.logging.logger
import java.util.*

class Qube(private val client: DefaultKubernetesClient, private val namespace: String) {
    private val log by logger()

    // This is needed because fabric8 isn't uptodate atm
    private val jobOps = JobOperationsImpl(client.httpClient, client.configuration, "v1", namespace, null,
        true, null, null, false, -1, TreeMap<String, String>(), TreeMap<String, String>(), TreeMap<String, Array<String>>(),
        TreeMap<String, Array<String>>(),
        TreeMap<String, String>())

    private val namespacedClient = client.inNamespace(namespace)

    fun deleteJob(job: Job) = jobOps.delete(job)
    fun createJob(job: Job) = jobOps.create(job)
    fun getJob(jobName: String) = jobOps.withName(jobName).get()
    fun listPodsForJob(job: Job) = namespacedClient.pods().withLabelSelector(job.spec.selector).list().items
    fun getLogs(podName: String) = namespacedClient.pods().withName(podName).getLog(true)
    fun ensureNamespaceExists(namespace: Namespace) = client.namespaces().createOrReplace(namespace)
    fun cloneClient() = Qube(DefaultKubernetesClient(client.configuration), namespace)

    fun watchEvents(f: (event: Event) -> Unit) {
        namespacedClient.events().watch(object: Watcher<Event> {
            override fun eventReceived(action: Watcher.Action?, resource: Event) = f(resource)

            override fun onClose(cause: KubernetesClientException?) {
                log.error("closed: {}", cause)
            }
        })
    }
}
