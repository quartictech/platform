package io.quartic.qube

import io.dropwizard.db.DataSourceFactory
import io.dropwizard.util.Duration
import io.fabric8.kubernetes.api.model.Pod
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec

data class KubernetesConfiguraration(
    val namespace: String,
    val podTemplate: Pod,
    val numConcurrentJobs: Int,
    val jobTimeoutSeconds: Long,
    val enable: Boolean,
    val deletePods: Boolean = true
)

data class DataSourceConfiguration(
    val user: String,
    val password: EncryptedSecret? = null,
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
    fun dataSourceFactory(secretsCodec: SecretsCodec): DataSourceFactory {
        val dataSourceFactory = DataSourceFactory()

        dataSourceFactory.user = user
        dataSourceFactory.password = if (password != null) secretsCodec.decrypt(password).veryUnsafe else null
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

        return dataSourceFactory
    }
}

data class DatabaseConfiguration(
    val runEmbedded: Boolean,
    val dataSource: DataSourceConfiguration
)

data class QubeConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val database: DatabaseConfiguration,
    val websocketPort: Int
) : ConfigurationBase()

