package io.quartic.qube

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.common.application.ApplicationBase
import io.quartic.common.client.retrofitClient
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.github.GithubInstallationClient
import io.quartic.qube.api.model.Dag
import io.quartic.qube.model.BuildJob
import io.quartic.qube.qube.JobPool
import io.quartic.qube.qube.Qube
import io.quartic.qube.resource.BackChannelResource
import io.quartic.qube.resource.QueryResource
import io.quartic.qube.resource.TriggerResource
import io.quartic.qube.store.BuildStore
import io.quartic.qube.store.setupDbi
import io.quartic.registry.api.RegistryServiceClient
import java.io.File
import java.util.concurrent.ArrayBlockingQueue

class QubeApplication : ApplicationBase<QubeConfiguration>() {
    val log by logger()
    override fun runApplication(configuration: QubeConfiguration, environment: Environment) {
        val queue = ArrayBlockingQueue<BuildJob>(1024)

        val registry = retrofitClient<RegistryServiceClient>(QubeApplication::class.java, configuration.registryUrl)

        val githubPrivateKey = configuration.secretsCodec.decrypt(configuration.github.privateKeyEncrypted)
        val githubClient = GithubInstallationClient(configuration.github.appId, configuration.github.apiRootUrl,
            githubPrivateKey)

        val buildStore = buildStore(environment, configuration.database, SecretsCodec(configuration.masterKeyBase64))

        if (configuration.kubernetes.enable) {
            val client = Qube(DefaultKubernetesClient(), configuration.kubernetes.namespace)
            JobPool(configuration.kubernetes, client, queue, buildStore, githubClient)
        } else {
            log.warn("Kubernetes is DISABLED. Jobs will NOT be run")
        }

        with (environment.jersey()) {
            // TODO: remove default pipeline
            register(QueryResource(buildStore,
                OBJECT_MAPPER.readValue(javaClass.getResourceAsStream("/pipeline.json"), Dag::class.java)))
            register(TriggerResource(queue, registry, buildStore))
            register(BackChannelResource(buildStore))
        }
    }

    private fun buildStore(environment: Environment, configuration: DatabaseConfiguration, secretsCodec: SecretsCodec): BuildStore {
         if (configuration.runEmbedded) {
             log.warn("Postgres is running in embedded mode!!")
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
