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
    private val migrationDatasource: DataSource,
    private val dbi: Jdbi,
    private val migrationVersion: MigrationVersion = MigrationVersion.LATEST
) {
    private val LOG by logger()

    constructor(
        datasourceFactory: DataSourceFactory,
        environment: Environment,
        migrationVersion: MigrationVersion = MigrationVersion.LATEST
    ) : this(
        datasourceFactory.build(environment.metrics(), "flyway"),
        JdbiFactory(TimedAnnotationNameStrategy()).build(environment, datasourceFactory, "postgres"),
        migrationVersion
    )

    constructor(
        owner: Class<*>,
        configuration: DatabaseConfiguration,
        environment: Environment,
        secretsCodec: SecretsCodec,
        migrationVersion: MigrationVersion = MigrationVersion.LATEST
    ) : this(
        datasourceFactory(owner, configuration, secretsCodec),
        environment,
        migrationVersion
    )

    val configuredDbi by lazy {
        migrate()
        setupDbi(dbi)
    }

    private fun migrate() {
        val flyway = Flyway()
        flyway.dataSource = migrationDatasource
        flyway.classLoader = javaClass.classLoader
        flyway.target = migrationVersion
        try {
            flyway.migrate()
        } catch (e: FlywayException) {
            LOG.error("Migration error", e)
            throw e
        }
    }

    inline fun <reified T> dao(): T = configuredDbi.onDemand(T::class.java)

    companion object {
        inline fun <reified T> testDao(
            dataSource: DataSource,
            migrationVersion: MigrationVersion = MigrationVersion.LATEST
        ): T {
            return DatabaseBuilder(dataSource, Jdbi.create(dataSource), migrationVersion).dao()
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
