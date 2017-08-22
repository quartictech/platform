package io.quartic.qube

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.model.Dag
import io.quartic.qube.pods.KubernetesClient
import io.quartic.qube.pods.Qubicle
import io.quartic.qube.resource.BackChannelResource
import io.quartic.qube.resource.QueryResource
import io.quartic.qube.store.JobStore
import io.quartic.qube.store.setupDbi
import io.vertx.core.Vertx
import java.io.File

class QubeApplication : ApplicationBase<QubeConfiguration>() {
    private val LOG by logger()
    override fun runApplication(configuration: QubeConfiguration, environment: Environment) {
        val jobStore = jobStore(environment, configuration.database, SecretsCodec(configuration.masterKeyBase64))

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
                    jobStore
                )
            )
        } else {
            LOG.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            // TODO: remove default pipeline
            register(QueryResource(
                OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)))
            register(BackChannelResource(jobStore))
        }

    }

    private fun jobStore(environment: Environment, configuration: DatabaseConfiguration, secretsCodec: SecretsCodec): JobStore {
         if (configuration.runEmbedded) {
             LOG.warn("Postgres is running in embedded mode!!")
             EmbeddedPostgres.builder()
                 .setPort(configuration.dataSource.port)
                 .setCleanDataDirectory(false)
                 .setDataDirectory(File("./data"))
                 .start()
        }
        val database = configuration.dataSource.dataSourceFactory(secretsCodec)
        JobStore.migrate(database.build(environment.metrics(), "flyway"))
        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "postgres")
        return setupDbi(dbi).onDemand(JobStore::class.java)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = QubeApplication().run(*args)
    }
}
