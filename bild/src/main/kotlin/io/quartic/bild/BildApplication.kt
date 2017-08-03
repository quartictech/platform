package io.quartic.bild

import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.model.BildJob
import io.quartic.bild.qube.JobPool
import io.quartic.bild.qube.Qube
import io.quartic.bild.resource.DagResource
import io.quartic.bild.resource.ExecResource
import io.quartic.common.application.ApplicationBase
import java.util.concurrent.ArrayBlockingQueue

class BildApplication : ApplicationBase<BildConfiguration>() {
    override fun runApplication(configuration: BildConfiguration, environment: Environment) {
        val queue = ArrayBlockingQueue<BildJob>(1024)
        val client = Qube(DefaultKubernetesClient(), configuration.kubernetes.namespace)

        val jobResults = JobResultStore()
        JobPool(configuration.kubernetes, client, queue, jobResults)

        with (environment.jersey()) {
            register(DagResource(jobResults))
            register(ExecResource(queue, jobResults))
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
