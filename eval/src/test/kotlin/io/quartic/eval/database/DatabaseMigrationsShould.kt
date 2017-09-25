package io.quartic.eval.database

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.BUILD_SUCCEEDED
import io.quartic.eval.database.model.BuildSucceeded
import io.quartic.eval.database.model.LegacyPhaseCompleted.*
import org.flywaydb.core.api.MigrationVersion
import org.hamcrest.Matcher
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
        val otherEventId = insertOtherEvent()

        databaseVersion("2")

        with(getEventFields(eventId)) {
            assertThat(this["id"] as UUID, equalTo(eventId))
            assertThat(this["build_id"] as UUID, equalTo(buildId))
            assertThat((this["time"] as Timestamp).toInstant(), equalTo(time))
            assertThat(OBJECT_MAPPER.readValue(this["payload"].toString()), equalTo(
                V2(
                    phaseId = phaseId,
                    result = V2.Result.Success(
                        V2.Artifact.EvaluationOutput(listOf(
                            V2.Node.Raw(
                                id = "0",
                                info = V2.LexicalInfo(
                                    name = "missing",
                                    description = "missing",
                                    file = "missing",
                                    lineRange = emptyList()
                                ),
                                output = V1.Dataset("y", "b"),
                                source = V2.Source.Bucket("b")
                            ),
                            V2.Node.Step(
                                id = "123",
                                info = V2.LexicalInfo(
                                    name = "alice",
                                    description = "foo",
                                    file = "foo.py",
                                    lineRange = listOf(10, 20)
                                ),
                                inputs = emptyList(),
                                output = V1.Dataset("x", "a")
                            ),
                            V2.Node.Step(
                                id = "456",
                                info = V2.LexicalInfo(
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

        assertThatOtherEventsArentNuked(otherEventId)
    }

    @Test
    fun v3_migrate() {
        databaseVersion("3")
    }

    @Test
    fun v4_migrate() {
        val eventId = uuid(100)
        val buildId = uuid(101)
        val phaseId = uuid(102)
        val time = Instant.now()
        insertEvent(eventId, buildId, time,
            V2(
                phaseId = phaseId,
                result = V2.Result.InternalError(RuntimeException("Load of rubbish"))
            )
        )
        val otherEventId = insertOtherEvent()

        databaseVersion("4")

        with(OBJECT_MAPPER.readValue<V4>(getEventFields(eventId)["payload"].toString())) {
            @Suppress("UNCHECKED_CAST")
            assertThat(this.result, isA(V4.Result.InternalError::class.java) as Matcher<V4.Result>)
        }
        assertThatOtherEventsArentNuked(otherEventId)
    }

    @Test
    fun v5_migrate() {
        val eventId = uuid(103)
        val buildId = uuid(104)
        val phaseId = uuid(105)
        val time = Instant.now()
        insertEvent(eventId, buildId, time,
            V4(
                phaseId = phaseId,
                result = V4.Result.UserError("wat")
            )
        )
        val otherEventId = insertOtherEvent()
        databaseVersion("5")

        with(OBJECT_MAPPER.readValue<V5>(getEventFields(eventId)["payload"].toString())) {
            @Suppress("UNCHECKED_CAST")
            assertThat(this.result, isA(V5.Result.UserError::class.java) as Matcher<V5.Result>)
            @Suppress("UNCHECKED_CAST")
            assertThat((this.result as V5.Result.UserError).info, isA(V5.UserErrorInfo.OtherException::class.java) as Matcher<V5.UserErrorInfo>)
        }
        assertThatOtherEventsArentNuked(otherEventId)
    }

    @Test
    fun v6_migrate() {
        databaseVersion("6")
    }

    private fun assertThatOtherEventsArentNuked(otherEventId: UUID) {
        assertThat(OBJECT_MAPPER.readValue(getEventFields(otherEventId)["payload"].toString()), isA(BuildSucceeded::class.java))
    }

    private fun getEventFields(eventId: UUID): Map<String, Any> {
        return handle.createQuery("""SELECT * FROM event WHERE id = :id""")
            .bind("id", eventId)
            .mapToMap()
            .toList()
            .single()
    }

    private fun insertOtherEvent(): UUID {
        val eventIdSucceeded = UUID.randomUUID()
        insertEvent(eventIdSucceeded, UUID.randomUUID(), Instant.now(), BUILD_SUCCEEDED)
        return eventIdSucceeded
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
