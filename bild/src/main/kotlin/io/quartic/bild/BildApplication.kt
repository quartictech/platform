package io.quartic.bild

import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.jdbi.DBIFactory
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
import io.quartic.github.GithubInstallationClient
import io.quartic.registry.api.RegistryServiceClient
import java.util.concurrent.ArrayBlockingQueue

class BildApplication : ApplicationBase<BildConfiguration>() {
    val log by logger()
    override fun runApplication(configuration: BildConfiguration, environment: Environment) {
        val queue = ArrayBlockingQueue<BildJob>(1024)

        val jobResults = JobResultStore()

        val registry = retrofitClient<RegistryServiceClient>(BildApplication::class.java, configuration.registryUrl)

        val githubPrivateKey = configuration.secretsCodec.decrypt(configuration.github.privateKeyEncrypted)
        val githubClient = GithubInstallationClient(configuration.github.appId, configuration.github.apiRootUrl,
            githubPrivateKey)

        if (configuration.kubernetes.enable) {
            val client = Qube(DefaultKubernetesClient(), configuration.kubernetes.namespace)
            JobPool(configuration.kubernetes, client, queue, jobResults, githubClient)
        } else {
            log.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        val dao = configureDatabase(configuration, environment)

        with (environment.jersey()) {
            register(QueryResource(jobResults, OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"))))
            register(TriggerResource(queue, registry))
            register(BackChannelResource(jobResults))
        }
    }

    private fun configureDatabase(configuration: BildConfiguration, environment: Environment): BildDao {
        val factory = DBIFactory()
        val jdbi = factory.build(environment, configuration.database, "postgresql")
        return jdbi.onDemand(BildDao::class.java)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
