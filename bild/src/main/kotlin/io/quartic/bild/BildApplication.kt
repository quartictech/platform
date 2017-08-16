package io.quartic.bild

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quartic.bild.api.model.Dag
import io.quartic.bild.model.BuildJob
import io.quartic.bild.qube.JobPool
import io.quartic.bild.qube.Qube
import io.quartic.bild.resource.BackChannelResource
import io.quartic.bild.resource.QueryResource
import io.quartic.bild.resource.TriggerResource
import io.quartic.bild.store.BuildStore
import io.quartic.bild.store.setupDbi
import io.quartic.common.application.ApplicationBase
import io.quartic.common.client.retrofitClient
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.github.GithubInstallationClient
import io.quartic.registry.api.RegistryServiceClient
import java.io.File
import java.util.concurrent.ArrayBlockingQueue

class BildApplication : ApplicationBase<BildConfiguration>() {
    val log by logger()
    override fun runApplication(configuration: BildConfiguration, environment: Environment) {
        val queue = ArrayBlockingQueue<BuildJob>(1024)

        val registry = retrofitClient<RegistryServiceClient>(BildApplication::class.java, configuration.registryUrl)

        val githubPrivateKey = configuration.secretsCodec.decrypt(configuration.github.privateKeyEncrypted)
        val githubClient = GithubInstallationClient(configuration.github.appId, configuration.github.apiRootUrl,
            githubPrivateKey)

        val buildStore = buildStore(environment, configuration.database)

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

    private fun buildStore(environment: Environment, configuration: DatabaseConfiguration): BuildStore {
         if (configuration.runEmbedded) {
            EmbeddedPostgres.builder()
                .setPort(configuration.dataSource.port)
                .setDataDirectory(File("./data"))
                .start()
        }
        val database = configuration.dataSource.dataSourceFactory
        BuildStore.migrate(database.build(environment.metrics(), "flyway"))
        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "postgres")
        return setupDbi(dbi).onDemand(BuildStore::class.java)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = BildApplication().run(*args)
    }
}
