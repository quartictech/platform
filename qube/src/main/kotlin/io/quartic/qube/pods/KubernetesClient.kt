package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.quartic.common.logging.logger

class KubernetesClient(private val client: DefaultKubernetesClient, private val namespace: String) {
    private val LOG by logger()

    private val namespacedClient = client.inNamespace(namespace)

    fun getLogs(podName: String) = namespacedClient.pods().withName(podName).getLog(true)
    fun ensureNamespaceExists() = client.namespaces().createOrReplace(
        NamespaceBuilder()
            .editOrNewMetadata()
            .withName(namespace)
            .endMetadata()
            .build()
    )
    fun createPod(pod: Pod) = namespacedClient.pods().create(pod)

    fun watchPod(podName:String, f: ( Watcher.Action?, Pod?) -> Any) = namespacedClient.pods().withName(podName).watch(object: Watcher<Pod>{
        override fun eventReceived(action: Watcher.Action?, resource: Pod?) {
           f(action, resource)
        }

        override fun onClose(cause: KubernetesClientException?) {
            LOG.error("Watch closed due to exception.", cause)
        }
    })

    fun deletePod(name: String) = namespacedClient.pods().withName(name).delete()
    fun getPod(name: String) = namespacedClient.pods().withName(name).get()
}
