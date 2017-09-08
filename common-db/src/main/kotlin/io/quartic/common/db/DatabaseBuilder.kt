package io.quartic.common.db

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.setup.Environment
import io.quartic.common.ApplicationDetails
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.MigrationVersion
import org.jdbi.v3.core.Jdbi
import javax.sql.DataSource

class DatabaseBuilder(
    private val owner: Class<*>,
    private val datasourceFactory: DataSourceFactory,
    private val environment: Environment,
    private val migrationVersion: MigrationVersion = MigrationVersion.LATEST
) {
    constructor(
        owner: Class<*>,
        configuration: DatabaseConfiguration,
        environment: Environment,
        secretsCodec: SecretsCodec,
        migrationVersion: MigrationVersion = MigrationVersion.LATEST
    ) : this(
        owner,
        datasourceFactory(owner, configuration, secretsCodec),
        environment,
        migrationVersion
    )

    val dbi by lazy {
        migrate(owner, datasourceFactory.build(environment.metrics(), "flyway"), migrationVersion)

        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, datasourceFactory, "postgres")

        setupDbi(dbi)
    }

    inline fun <reified T> dao(): T = dbi.onDemand(T::class.java)

    companion object {
        val LOG by logger()

        fun migrate(owner: Class<*>, dataSource: DataSource, migrationVersion: MigrationVersion) {
            val flyway = Flyway()
            flyway.dataSource = dataSource
            flyway.classLoader = owner.classLoader
            flyway.target = migrationVersion
            try {
                flyway.migrate()
            } catch (e: FlywayException) {
                LOG.error("Migration error", e)
                throw e
            }
        }

        inline fun <reified T> testDao(
            owner: Class<*>,
            dataSource: DataSource,
            migrationVersion: MigrationVersion = MigrationVersion.LATEST
        ): T {
            migrate(owner, dataSource, migrationVersion)
            val dbi = Jdbi.create(dataSource)
            return setupDbi(dbi).onDemand(T::class.java)
        }

        private fun datasourceFactory(
            owner: Class<*>,
            configuration: DatabaseConfiguration,
            secretsCodec: SecretsCodec
        ): DataSourceFactory {
            val details = ApplicationDetails(owner)
            val factory = configuration.dataSourceFactory(secretsCodec)
            factory.validationQuery = "/* ${details.name} - ${details.version} Health Check */ SELECT 1"
            factory.properties = mutableMapOf("ApplicationName" to "${details.name} - ${details.version}")
            return factory
        }
    }
}
