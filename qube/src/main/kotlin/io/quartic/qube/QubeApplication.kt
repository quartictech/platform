package io.quartic.qube

import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.db.DatabaseBuilder
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.Qubicle
import io.quartic.qube.resource.BackChannelResource
import io.quartic.qube.store.JobStore
import io.vertx.core.Vertx

class QubeApplication : ApplicationBase<QubeConfiguration>() {
    private val LOG by logger()
    override fun runApplication(configuration: QubeConfiguration, environment: Environment) {
        val databaseBuilder = DatabaseBuilder(
            QubeApplication::class.java,
            configuration.database,
            environment,
            SecretsCodec(configuration.masterKeyBase64)
        )

        val jobStore = databaseBuilder.dao<JobStore>()

        if (configuration.kubernetes.enable) {
            val client = KubernetesClient(DefaultKubernetesClient(), configuration.kubernetes.namespace)
            client.ensureNamespaceExists()

            val vertx = Vertx.vertx()
            vertx.deployVerticle(
                Qubicle(
                    configuration.websocketPort,
                    client,
                    configuration.kubernetes.podTemplate,
                    configuration.kubernetes.namespace,
                    configuration.kubernetes.numConcurrentJobs,
                    configuration.kubernetes.jobTimeoutSeconds,
                    configuration.kubernetes.deletePods,
                    jobStore
                )
            )
        } else {
            LOG.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            register(BackChannelResource())
        }

    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = QubeApplication().run(*args)
    }
}
