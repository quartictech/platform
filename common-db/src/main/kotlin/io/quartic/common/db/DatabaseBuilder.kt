package io.quartic.common.db

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
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
    val owner: Class<*>,
    val configuration: DatabaseConfiguration,
    val environment: Environment,
    val secretsCodec: SecretsCodec) {


    inline fun <reified T> dao(): T {
        val factory = configuration.dataSourceFactory(secretsCodec)

        val details = ApplicationDetails(owner)
        factory.validationQuery = "/* ${details.name} - ${details.version} Health Check */ SELECT 1"
        factory.properties = mutableMapOf(
            "ApplicationName" to "${details.name} - ${details.version}"
        )

        migrate(owner, factory.build(environment.metrics(), "flyway"), MigrationVersion.LATEST)

        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, factory, "postgres")
        return setupDbi(dbi).onDemand(T::class.java)
    }

    companion object {
        val LOG by logger()

        fun migrate(owner: Class<*>, dataSource: DataSource,
                    migrationVersion: MigrationVersion) {
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

        inline fun <reified T> testDao(owner: Class<*>, dataSource: DataSource,
                                       migrationVersion: MigrationVersion = MigrationVersion.LATEST): T {
            migrate(owner, dataSource, migrationVersion)
            val dbi = Jdbi.create(dataSource)
            return setupDbi(dbi).onDemand(T::class.java)
        }
    }
}
