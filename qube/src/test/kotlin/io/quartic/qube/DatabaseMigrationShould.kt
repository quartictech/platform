package io.quartic.qube

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.model.ContainerSpec
import io.quartic.qube.api.model.ContainerState
import io.quartic.qube.api.model.PodSpec
import org.flywaydb.core.api.MigrationVersion
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.time.Instant
import java.util.*

class DatabaseMigrationShould {
    @Test
    fun migrate_to_v2() {
        databaseVersion("1")
        val id = UUID.randomUUID()
        val clientId = UUID.randomUUID()
        DBI.open().createUpdate("""
            INSERT INTO job(id, client, name, create_spec, log, start_time, end_time, reason, message, exit_code)
            VALUES (:id, :client, :name, :create_spec, :log, :start_time, :end_time, :reason, :message, :exit_code)
        """)
            .bind("id", id)
            .bind("client", clientId)
            .bind("name", "noob")
            .bindJson("create_spec",
                QubeRequest.Create(
                    "noob",
                    PodSpec(listOf(
                        ContainerSpec("container", "dummy:1", listOf("true"), 8000))
                    )
                ))
            .bind("log", "logs")
            .bind("start_time", Instant.now())
            .bind("end_time", Instant.now())
            .bind("reason", "thing")
            .bind("message", "mess")
            .bind("exit_code", 1)
            .execute()
        databaseVersion("2")

        val row = DBI.open().createQuery("SELECT containers FROM job")
            .map { rs, _ -> rs.getString("containers")}
            .findOnly()
        val expected = mapOf("default" to ContainerState(1, "thing", "mess", "logs"))
        assertThat(OBJECT_MAPPER.readValue<Map<String, ContainerState>>(row as String), equalTo(expected))
    }

    private fun databaseVersion(version: String): Database = DatabaseBuilder
        .testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase,
            MigrationVersion.fromVersion(version))

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
