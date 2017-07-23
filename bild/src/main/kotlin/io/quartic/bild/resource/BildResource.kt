package io.quartic.bild.resource

import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.quartic.bild.qube.QubeClient
import java.util.*
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/exec")
class BildResource(val template: Job) {
    val client = QubeClient()




    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun exec() {
       val ns = NamespaceBuilder().withNewMetadata().withName("bild").endMetadata().build()
        println("creating namespace")
        DefaultKubernetesClient().use.namespaces().createOrReplace(ns)
        val jobName = "pipeline-${UUID.randomUUID()}"
        val job = JobBuilder(template).withNewMetadata()
            .withNamespace("bild")
            .withName(jobName)
            .withLabels(mapOf(Pair("job-name", jobName)))
            .endMetadata()
            .build()
        val watch = kubernetesClient.pods().withLabel("job-name", jobName).watch(object: Watcher<Pod>{
            override fun onClose(cause: KubernetesClientException?) {

                println("closed")
            }

            override fun eventReceived(action: Watcher.Action, resource: Pod) {
                println(action)
                println(resource)
            }
        })
    }
}
