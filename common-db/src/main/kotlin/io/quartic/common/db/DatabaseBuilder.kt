package io.quartic.common.db

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.setup.Environment
import io.quartic.common.ApplicationDetails
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.jdbi.v3.core.Jdbi
import java.io.File
import javax.sql.DataSource

class DatabaseBuilder(
    val owner: Class<*>,
    val configuration: DatabaseConfiguration,
    val environment: Environment,
    val secretsCodec: SecretsCodec) {
    val LOG by logger()

    fun launchEmbeddedPostgres(): DataSourceFactory {
            LOG.warn("\n" + """
                #####################################################################
                #                                                                   #
                #               !!! RUNNING WITH EMBEDDED POSTGRES !!!              #
                #                                                                   #
                #####################################################################
            """.trimIndent())
            val postgres = EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(File("./data"))
                .start()
            return configuration.dataSource.dataSourceFactory(secretsCodec, postgres.port)
    }

    inline fun <reified T> dao(): T {
        val database = if (configuration.runEmbedded) {
            launchEmbeddedPostgres()
        } else {
            configuration.dataSource.dataSourceFactory(secretsCodec)
        }

        val details = ApplicationDetails(owner)
        database.validationQuery = "/* ${details.name} - ${details.version} Health Check */ SELECT 1"
        database.properties = mutableMapOf(
            "ApplicationName" to "${details.name} - ${details.version}"
        )

        migrate(owner, database.build(environment.metrics(), "flyway"), MigrationVersion.LATEST)

        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "postgres")
        return setupDbi(dbi).onDemand(T::class.java)
    }

    companion object {
        fun migrate(owner: Class<*>, dataSource: DataSource,
                    migrationVersion: MigrationVersion) {
            val flyway = Flyway()
            flyway.dataSource = dataSource
            flyway.classLoader = owner.classLoader
            flyway.target = migrationVersion
            flyway.migrate()
        }

        inline fun <reified T> testDao(owner: Class<*>, dataSource: DataSource,
                                       migrationVersion: MigrationVersion = MigrationVersion.LATEST): T {
            migrate(owner, dataSource, migrationVersion)
            val dbi = Jdbi.create(dataSource)
            return setupDbi(dbi).onDemand(T::class.java)
        }
    }
}
