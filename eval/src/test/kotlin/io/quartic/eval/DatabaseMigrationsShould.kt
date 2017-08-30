package io.quartic.eval

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.TriggerDetails
import org.flywaydb.core.api.MigrationVersion
import org.jdbi.v3.core.Jdbi
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.net.URI
import java.time.Instant
import java.util.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.CoreMatchers.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DatabaseMigrationsShould {
    @Test
    fun v1_migrate() {
        databaseVersion("1")
    }

    @Test
    fun v2_migrate() {
        DBI.open().createUpdate("""
            insert into build(id, customer_id, trigger_details, build_number, time)
            values(:id, :customer_id, :trigger_details, :build_number, :time)
            """)
            .bind("id", UUID.randomUUID())
            .bind("customer_id", customerId)
            .bindJson("trigger_details", triggerDetails)
            .bind("build_number", 1)
            .bind("time", Instant.now())
            .execute()

        databaseVersion("2")
        DBI.open().createQuery("select branch from build")
            .mapTo(String::class.java)
            .forEach { assertThat(it, equalTo("develop")) }
    }

    fun databaseVersion(version: String): Database = DatabaseBuilder
        .testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase,
            MigrationVersion.fromVersion(version))

    val customerId = CustomerId(100)
    val branch = "develop"
    private val triggerDetails = TriggerDetails(
        type = "wat",
        deliveryId = "id",
        installationId = 100,
        repoId = 100,
        repoName = "repo",
        cloneUrl = URI.create("ref"),
        ref = "refs/heads/${branch}",
        commit = "commit",
        timestamp = Instant.now()
    )


    companion object {
        @ClassRule
        @JvmField
        val PG = EmbeddedPostgresRules.singleInstance()

        private lateinit var DBI: Jdbi

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DBI = setupDbi(Jdbi.create(PG.embeddedPostgres.postgresDatabase))
        }
    }
}