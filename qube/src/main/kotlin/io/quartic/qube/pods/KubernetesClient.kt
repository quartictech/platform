package io.quartic.qube.pods

import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.quartic.common.logging.logger
import kotlinx.coroutines.experimental.channels.Channel

class KubernetesClient(private val client: DefaultKubernetesClient, private val namespace: String) {
    private val LOG by logger()

    data class PodWatch(
        val channel: Channel<Pod>,
        private val watch: Watch
    ) {
        fun close() {
            watch.close()
        }
    }

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

    fun watchPod(podName:String): PodWatch {
        val channel = Channel<Pod>()
        val watch = namespacedClient.pods().withName(podName)
            .watch(object: Watcher<Pod>{
                override fun eventReceived(action: Watcher.Action?, resource: Pod?) {
                    if (resource != null) {
                        channel.offer(resource)
                    } else {
                        LOG.warn("[{}] Received null pod from watch.", podName)
                    }
                }

                override fun onClose(cause: KubernetesClientException?) {
                    LOG.error("Watch closed due to exception.", cause)
                    channel.close()
                }
            })
        return PodWatch(channel, watch)
    }

    fun deletePod(name: String) = namespacedClient.pods().withName(name).delete()
    fun getPod(name: String) = namespacedClient.pods().withName(name).get()
}
