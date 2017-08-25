package io.quartic.db

import io.dropwizard.db.DataSourceFactory
import io.dropwizard.util.Duration
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.secrets.SecretsCodec

data class DatabaseConfiguration(
    val runEmbedded: Boolean,
    val dataSource: DataSourceConfiguration
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
    val minSize: Int = 8,
    val maxSize: Int = 32,
    val checkConnectionWhileIdle: Boolean = false,
    val evictionInterval: Duration = Duration.seconds(10),
    val minIdleTime: Duration = Duration.minutes(1)
) {
    fun dataSourceFactory(secretsCodec: SecretsCodec, overridePort: Int = port): DataSourceFactory {
        val dataSourceFactory = DataSourceFactory()

        dataSourceFactory.user = user
        dataSourceFactory.password = if (password != null) secretsCodec.decrypt(password).veryUnsafe else null
        dataSourceFactory.url = "jdbc:postgresql://${hostName}:${overridePort}/${databaseName}"
        dataSourceFactory.driverClass = driverClass
        dataSourceFactory.properties = properties
        dataSourceFactory.maxWaitForConnection = maxWaitForConnection
        dataSourceFactory.minSize = minSize
        dataSourceFactory.maxSize = maxSize
        dataSourceFactory.checkConnectionWhileIdle = checkConnectionWhileIdle
        dataSourceFactory.evictionInterval = evictionInterval
        dataSourceFactory.minIdleTime = minIdleTime

        return dataSourceFactory
    }
}
