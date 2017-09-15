package io.quartic.eval.database

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.BUILD_SUCCEEDED
import io.quartic.eval.database.model.BuildSucceeded
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.LexicalInfo
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node.Raw
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node.Step
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.Success
import io.quartic.eval.database.model.CurrentPhaseCompleted.Source.Bucket
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1
import io.quartic.eval.database.model.PhaseCompleted
import org.flywaydb.core.api.MigrationVersion
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.isA
import org.jdbi.v3.core.Jdbi
import org.junit.Assert.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DatabaseMigrationsShould {
    private val handle = DBI.open()

    @Test
    fun v1_migrate() {
        databaseVersion("1")
    }

    @Test
    fun v2_migrate() {
        val eventId = uuid(100)
        val buildId = uuid(101)
        val phaseId = uuid(102)
        val time = Instant.now()
        insertEvent(eventId, buildId, time,
            V1(
                phaseId,
                V1.Result.Success(
                    V1.Artifact.EvaluationOutput(listOf(
                        V1.Step(
                            id = "123",
                            name = "alice",
                            description = "foo",
                            file = "foo.py",
                            lineRange = listOf(10, 20),
                            inputs = emptyList(),
                            outputs = listOf(V1.Dataset("x", "a"))
                        ),
                        V1.Step(
                            id = "456",
                            name = "bob",
                            description = "bar",
                            file = "bar.py",
                            lineRange = listOf(30, 40),
                            inputs = listOf(V1.Dataset("x", "a"), V1.Dataset("y", "b")),
                            outputs = listOf(V1.Dataset("z", "c"))
                        )
                    ))
                )
            )
        )

        val eventIdSucceeded = uuid(103)
        insertEvent(eventIdSucceeded, UUID.randomUUID(), Instant.now(), BUILD_SUCCEEDED)

        databaseVersion("2")

        with(getEventFields(eventId)) {
            assertThat(this["id"] as UUID, equalTo(eventId))
            assertThat(this["build_id"] as UUID, equalTo(buildId))
            assertThat((this["time"] as Timestamp).toInstant(), equalTo(time))
            assertThat(OBJECT_MAPPER.readValue(this["payload"].toString()), equalTo(
                PhaseCompleted(
                    phaseId = phaseId,
                    result = Success(
                        EvaluationOutput(listOf(
                            Raw(
                                id = "0",
                                info = LexicalInfo(
                                    name = "missing",
                                    description = "missing",
                                    file = "missing",
                                    lineRange = emptyList()
                                ),
                                output = V1.Dataset("y", "b"),
                                source = Bucket("b")
                            ),
                            Step(
                                id = "123",
                                info = LexicalInfo(
                                    name = "alice",
                                    description = "foo",
                                    file = "foo.py",
                                    lineRange = listOf(10, 20)
                                ),
                                inputs = emptyList(),
                                output = V1.Dataset("x", "a")
                            ),
                            Step(
                                id = "456",
                                info = LexicalInfo(
                                    name = "bob",
                                    description = "bar",
                                    file = "bar.py",
                                    lineRange = listOf(30, 40)
                                ),
                                inputs = listOf(V1.Dataset("x", "a"), V1.Dataset("y", "b")),
                                output = V1.Dataset("z", "c")
                            )
                        ))
                    )
                )
            ))
        }

        // Ensure that other rows aren't nuked
        assertThat(OBJECT_MAPPER.readValue(getEventFields(eventIdSucceeded)["payload"].toString()), isA(BuildSucceeded::class.java))
    }

    @Test
    fun v3_migrate() {
        databaseVersion("3")
    }

    private fun getEventFields(eventId: UUID): Map<String, Any> {
        return handle.createQuery("""SELECT * FROM event WHERE id = :id""")
            .bind("id", eventId)
            .mapToMap()
            .toList()
            .single()
    }

    private fun insertEvent(eventId: UUID, buildId: UUID, time: Instant, payload: Any) {
        handle.createUpdate("""
            INSERT INTO event (id, build_id, payload, time)
                VALUES (:id, :build_id, :payload, :time)
            """)
            .bind("id", eventId)
            .bind("build_id", buildId)
            .bindJson("payload", payload)
            .bind("time", time)
            .execute()
    }

    private fun databaseVersion(version: String): Database = DatabaseBuilder
        .testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase,
            MigrationVersion.fromVersion(version))

    private fun uuid(x: Int) = UUID(0, x.toLong())

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
