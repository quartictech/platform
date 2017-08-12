package io.quartic.bild

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.setup.Environment
import io.fabric8.kubernetes.api.model.Job
import io.quartic.bild.store.EmbeddedJobResultStore
import io.quartic.bild.store.JobResultStore
import io.quartic.bild.store.PostgresJobResultStore
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.github.arteam.jdbi3.JdbiFactory
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin


data class KubernetesConfiguraration(
    val namespace: String,
    val template: Job,
    val numConcurrentJobs: Int,
    val maxFailures: Int,
    val creationTimeoutSeconds: Int,
    val runTimeoutSeconds: Int,
    val backChannelEndpoint: String,
    val enable: Boolean
)

data class GitHubConfiguration(
    val appId: String,
    val apiRootUrl: String,
    val privateKeyEncrypted: EncryptedSecret
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = StoreConfiguration.PostgresStoreConfiguration::class, name = "postgres"),
    JsonSubTypes.Type(value = StoreConfiguration.EmbeddedStoreConfiguration::class, name = "embedded")
)
sealed class StoreConfiguration {
    abstract fun create(environment: Environment): JobResultStore

    data class PostgresStoreConfiguration(
        val database: DataSourceFactory
    ) : StoreConfiguration() {
        override fun create(environment: Environment): JobResultStore {
            val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "hsql")
            dbi.installPlugin(KotlinPlugin())
            return PostgresJobResultStore(database.build(environment.metrics(), "flyway"), dbi)
        }
    }

    class EmbeddedStoreConfiguration : StoreConfiguration() {
        override fun create(environment: Environment): JobResultStore {
            return EmbeddedJobResultStore()
        }
    }
}

data class BildConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val store: StoreConfiguration,
    val registryUrl: String,
    val github: GitHubConfiguration
) : ConfigurationBase()

