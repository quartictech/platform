package io.quartic.bild.resource

import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.api.model.JobBuilder
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.JobOperationsImpl
import io.fabric8.openshift.api.model.OAuthClient
import io.quartic.bild.qube.QubeClient
import java.util.*
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/exec")
class BildResource(val template: Job) {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun exec() {
        println("creating namespace")
        DefaultKubernetesClient().use { client ->
            val ns = NamespaceBuilder().withNewMetadata().withName("bild").endMetadata().build()
            client.namespaces().createOrReplace(ns)
            QubeClient(client).runJob("bild", template)
        }
    }
}

