package io.quartic.qube

import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.common.application.ApplicationBase
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.logging.logger
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.OrchestratorState
import io.quartic.qube.pods.Qubicle
import io.quartic.qube.resource.ApiResource
import io.quartic.qube.resource.BackChannelResource
import io.vertx.core.Vertx

class QubeApplication : ApplicationBase<QubeConfiguration>() {
    private val LOG by logger()
    override fun runApplication(configuration: QubeConfiguration, environment: Environment) {
        val databaseBuilder = DatabaseBuilder(
            javaClass,
            configuration.database,
            environment,
            configuration.secretsCodec
        )

        val jobStore = databaseBuilder.dao<Database>()
        val orchestratorState = OrchestratorState()

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
                    jobStore,
                    orchestratorState
                )
            )
        } else {
            LOG.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            register(BackChannelResource())
            register(ApiResource(orchestratorState))
        }

    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = QubeApplication().run(*args)
    }
}
