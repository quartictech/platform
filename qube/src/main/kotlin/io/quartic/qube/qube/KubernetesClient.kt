package io.quartic.qube.qube

import io.fabric8.kubernetes.api.model.Event
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.quartic.common.logging.logger

class KubernetesClient(private val client: DefaultKubernetesClient, private val namespace: String) {
    private val log by logger()

    private val namespacedClient = client.inNamespace(namespace)

    fun deleteJob(job: Job) = client.extensions().jobs().delete(job)
    fun createJob(job: Job) = client.extensions().jobs().create(job)
    fun getJob(jobName: String) = client.extensions().jobs().withName(jobName).get()
    fun listPodsForJob(job: Job) = namespacedClient.pods().withLabelSelector(job.spec.selector).list().items
    fun getLogs(podName: String) = namespacedClient.pods().withName(podName).getLog(true)
    fun ensureNamespaceExists(namespace: Namespace) = client.namespaces().createOrReplace(namespace)

    fun createPod(pod: Pod) = namespacedClient.pods().create(pod)

    fun watchPod(podName:String, f: ( Watcher.Action?, Pod?) -> Any) = namespacedClient.pods().withName(podName).watch(object: Watcher<Pod>{
        override fun eventReceived(action: Watcher.Action?, resource: Pod?) {
           f(action, resource)
        }

        override fun onClose(cause: KubernetesClientException?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    })

    fun watchEvents(f: (event: Event) -> Unit) {
        namespacedClient.events().watch(object: Watcher<Event> {
            override fun eventReceived(action: Watcher.Action?, resource: Event) = f(resource)

            override fun onClose(cause: KubernetesClientException?) {
                log.error("closed: {}", cause)
            }
        })
    }

    fun deletePod(name: String) = namespacedClient.pods().withName(name).delete()
}
