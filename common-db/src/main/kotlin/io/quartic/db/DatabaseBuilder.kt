package io.quartic.db

import com.github.arteam.jdbi3.JdbiFactory
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.dropwizard.setup.Environment
import io.quartic.common.ApplicationDetails
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import org.flywaydb.core.Flyway
import java.io.File

class DatabaseBuilder(
    val owner: Class<*>,
    val configuration: DatabaseConfiguration,
    val environment: Environment,
    val secretsCodec: SecretsCodec) {
    val LOG by logger()

    inline fun <reified T> dao(): T {
        val database = if (configuration.runEmbedded) {
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
            configuration.dataSource.dataSourceFactory(secretsCodec, postgres.port)
        } else {
           configuration.dataSource.dataSourceFactory(secretsCodec)
        }

        val details = ApplicationDetails(owner)
        database.validationQuery = "/* ${details.name} / ${details.version} Health Check */ SELECT 1"

        val flyway = Flyway()
        flyway.dataSource = database.build(environment.metrics(), "flyway")
        flyway.classLoader = owner.classLoader
        flyway.migrate()

        val dbi = JdbiFactory(TimedAnnotationNameStrategy()).build(environment, database, "postgres")
        return setupDbi(dbi).onDemand(T::class.java)
    }
}
