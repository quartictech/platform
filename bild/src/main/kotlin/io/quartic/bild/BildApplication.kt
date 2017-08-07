package io.quartic.bild

import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.model.BildJob
import io.quartic.bild.qube.JobPool
import io.quartic.bild.qube.Qube
import io.quartic.bild.resource.BackChannelResource
import io.quartic.bild.resource.QueryResource
import io.quartic.bild.resource.TriggerResource
import io.quartic.common.application.ApplicationBase
import io.quartic.common.client.retrofitClient
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.registry.api.RegistryServiceAsync
import sun.misc.BASE64Decoder
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

class BildApplication : ApplicationBase<BildConfiguration>() {
    val log by logger()
    override fun runApplication(configuration: BildConfiguration, environment: Environment) {
        val queue = ArrayBlockingQueue<BildJob>(1024)

        val jobResults = JobResultStore()

        val registry = retrofitClient<RegistryServiceAsync>(BildApplication::class.java, configuration.registryUrl)

        val githubClient = GithubInstallationClient(configuration.github.appId, configuration.github.apiRootUrl,
            configuration.github.privateKey)

        if (configuration.kubernetes.enable) {
            val client = Qube(DefaultKubernetesClient(), configuration.kubernetes.namespace)
            JobPool(configuration.kubernetes, client, queue, jobResults, githubClient)
        } else {
            log.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            register(QueryResource(jobResults, OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"))))
            register(TriggerResource(queue, registry))
            register(BackChannelResource(jobResults))
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
