package io.quartic.bild

import io.dropwizard.db.DataSourceFactory
import io.dropwizard.util.Duration
import io.fabric8.kubernetes.api.model.Job
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret

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

data class DataSourceConfiguration(
    val user: String,
    val password: String,
    val hostName: String = "localhost",
    val port: Int = 5432,
    val databaseName: String = "postgres",
    val driverClass: String = "org.postgresql.Driver",
    val properties: Map<String, String> = mapOf("charSet" to "UTF-8"),
    val maxWaitForConnection: Duration = Duration.seconds(1),
    val validationQuery: String = "/* MyService Health Check */ SELECT 1",
    val minSize: Int = 8,
    val maxSize: Int = 32,
    val checkConnectionWhileIdle: Boolean = false,
    val evictionInterval: Duration = Duration.seconds(10),
    val minIdleTime: Duration = Duration.minutes(1)
) {

    val dataSourceFactory = DataSourceFactory()

    init {
        dataSourceFactory.user = user
        dataSourceFactory.password = password
        dataSourceFactory.url = "jdbc:postgresql://${hostName}:${port}/${databaseName}"
        dataSourceFactory.driverClass = driverClass
        dataSourceFactory.properties = properties
        dataSourceFactory.maxWaitForConnection = maxWaitForConnection
        dataSourceFactory.validationQuery = validationQuery
        dataSourceFactory.minSize = minSize
        dataSourceFactory.maxSize = maxSize
        dataSourceFactory.checkConnectionWhileIdle = checkConnectionWhileIdle
        dataSourceFactory.evictionInterval = evictionInterval
        dataSourceFactory.minIdleTime = minIdleTime
    }
}

data class DatabaseConfiguration(
    val runEmbedded: Boolean,
    val dataSource: DataSourceConfiguration
)

data class BildConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val database: DatabaseConfiguration,
    val registryUrl: String,
    val github: GitHubConfiguration
) : ConfigurationBase()

