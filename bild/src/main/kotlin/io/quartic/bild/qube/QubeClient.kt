package io.quartic.bild.qube

import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.quartic.common.logging.logger
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class QubeClient(val client: DefaultKubernetesClient) {
    val logger by logger()

     fun jobs() = JobOperationsImpl(client.httpClient, client.configuration, "v1", "bild", null,
         true, null, null, false, -1, TreeMap<String, String>(), TreeMap<String, String>(), TreeMap<String, Array<String>>(),
            TreeMap<String, Array<String>>(),
            TreeMap<String, String>())

    fun runJob(namespace: String, jobTemplate: Job) {
//        thread(start = true) {
            val closeLatch = CountDownLatch(1);
            try {
                val jobName = "pipeline-${UUID.randomUUID()}"
                val job = JobBuilder(jobTemplate).withNewMetadata()
                    .withNamespace(namespace)
                    .withName(jobName)
                    .endMetadata()
                    .build()
                val jobResult = jobs().create(job)
                val jobSelector = jobResult.spec.selector

                client.pods().inNamespace(namespace).withLabelSelector(jobSelector).watch(object : Watcher<Pod> {
                    override fun eventReceived(action: Watcher.Action?, resource: Pod?) {
                        logger.info("{}: {}", action, resource?.metadata?.resourceVersion)
                        logger.info("{}", resource?.status?.containerStatuses?.)
                    }

                    override fun onClose(cause: KubernetesClientException?) {
                        logger.debug("Watcher onClose")
                        if (cause != null) {
                            logger.error(cause.message, cause)
                            closeLatch.countDown()
                        }
                    }
                }).use { watch ->
                    closeLatch.await(30, TimeUnit.SECONDS)
                }
            } catch (e: KubernetesClientException) {
                e.printStackTrace();
                logger.error("Could not watch resources", e);
            }
        }
//    }
}
