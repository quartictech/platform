package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.model.ContainerAcquired
import io.quartic.eval.model.TriggerReceived
import org.flywaydb.core.api.MigrationVersion
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.time.Instant
import java.util.*

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
            .bindJson("trigger_details", buildTrigger)
            .bind("build_number", 1)
            .bind("time", Instant.now())
            .execute()

        databaseVersion("2")
        DBI.open().createQuery("select branch from build")
            .mapTo(String::class.java)
            .forEach { assertThat(it, equalTo("develop")) }
    }

    @Test
    fun v3_migrate() {
        databaseVersion("3")
        assertThat(checkTableExists("phase", "public"), equalTo(false))
        assertThat(checkTableExists("build", "public"), equalTo(true))
        assertThat(checkTableExists("event", "public"), equalTo(true))
    }

    @Test
    fun v4_migrate() {
        databaseVersion("4")
    }

    @Test
    fun v5_migrate() {
        databaseVersion("5")
    }

    @Test
    fun v6_migrate() {

        DBI.open().createUpdate("""
            INSERT INTO event(id, build_id, time, payload)
                VALUES(:id, :build_id, :time, :payload)
        """)
            .bind("id", UUID.randomUUID())
            .bind("build_id", UUID.randomUUID())
            .bind("time", Instant.now())
            .bindJson("payload", mapOf("type" to "container_acquired_v1", "hostname" to "a.b.c"))
            .execute()

        databaseVersion("6")

        val results = DBI.open().createQuery("SELECT payload FROM event")
            .mapTo(String::class.java)
            .map { OBJECT_MAPPER.readValue<ContainerAcquired>(it) }

        assertThat(results[0], equalTo(ContainerAcquired(UUID(0, 0), "a.b.c")))
    }

    @Test
    fun v7_migrate() {
        databaseVersion("6")
        val oldTriggerFormat = javaClass.getResource("/trigger_received.json").readText()
        val id = UUID.randomUUID()
        DBI.open().createUpdate("""
            INSERT INTO event(id, build_id, time, payload)
            VALUES(:id, :build_id, :time, :payload)
            """)
            .bind("id", id)
            .bind("build_id", UUID.randomUUID())
            .bind("time", Instant.now())
            .bindJson("payload", OBJECT_MAPPER.readValue(oldTriggerFormat))
            .execute()

        databaseVersion("7")

        val trigger = OBJECT_MAPPER.readValue<TriggerReceived>(DBI.open()
            .createQuery("select payload from event where id = :id")
            .bind("id", id)
            .mapTo(String::class.java)
            .findOnly())

        val githubWebhook: BuildTrigger.GithubWebhook = trigger.trigger as BuildTrigger.GithubWebhook
        assertThat(githubWebhook, notNullValue())
    }



    private fun checkTableExists(name: String, schema: String): Boolean =
        DBI.open().createQuery(
            """select exists(
            select 1
            from pg_tables
            where schemaname = :schema_name
            and tablename = :table_name
            );""")
            .bind("table_name", name)
            .bind("schema_name", schema)
            .mapTo(Boolean::class.java)
            .findOnly()

    private fun databaseVersion(version: String): Database = DatabaseBuilder
        .testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase,
            MigrationVersion.fromVersion(version))

    private val customerId = CustomerId(100)
    private val branch = "develop"
    private val buildTrigger = BuildTrigger.GithubWebhook(
        deliveryId = "id",
        installationId = 100,
        repoId = 100,
        repoName = "repo",
        repoOwner = "my",
        ref = "refs/heads/${branch}",
        commit = "commit",
        timestamp = Instant.now(),
        rawWebhook = emptyMap()
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
