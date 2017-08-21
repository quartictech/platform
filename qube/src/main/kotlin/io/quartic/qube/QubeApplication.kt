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
import io.quartic.qube.store.BuildStore
import io.quartic.qube.store.setupDbi
import io.vertx.core.Vertx
import java.io.File

class QubeApplication : ApplicationBase<QubeConfiguration>() {
    val LOG by logger()
    override fun runApplication(configuration: QubeConfiguration, environment: Environment) {
        val buildStore = buildStore(environment, configuration.database, SecretsCodec(configuration.masterKeyBase64))

        if (configuration.kubernetes.enable) {
            val client = KubernetesClient(DefaultKubernetesClient(), configuration.kubernetes.namespace)
            val namespace = NamespaceBuilder()
                .editOrNewMetadata()
                .withName(configuration.kubernetes.namespace)
                .endMetadata()
                .build()
            client.ensureNamespaceExists(namespace)

            val vertx = Vertx.vertx()
            vertx.deployVerticle(Qubicle(client, configuration.kubernetes.podTemplate,
                configuration.kubernetes.namespace, configuration.kubernetes.numConcurrentJobs))
        } else {
            LOG.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            // TODO: remove default pipeline
            register(QueryResource(buildStore,
                OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)))
            register(BackChannelResource(buildStore))
        }

    }

    private fun buildStore(environment: Environment, configuration: DatabaseConfiguration, secretsCodec: SecretsCodec): BuildStore {
         if (configuration.runEmbedded) {
             LOG.warn("Postgres is running in embedded mode!!")
             EmbeddedPostgres.builder()
                 .setPort(configuration.dataSource.port)
                 .setCleanDataDirectory(false)
                 .setDataDirectory(File("./data"))
                 .start()
        }
        val database = configuration.dataSource.dataSourceFactory(secretsCodec)
        BuildStore.migrate(database.build(environment.metrics(), "flyway"))
        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "postgres")
        return setupDbi(dbi).onDemand(BuildStore::class.java)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = QubeApplication().run(*args)
    }
}
