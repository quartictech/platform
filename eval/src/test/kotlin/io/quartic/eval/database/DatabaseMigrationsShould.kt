package io.quartic.eval.database

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V6.Artifact.NodeExecution
import io.quartic.eval.database.model.LegacyPhaseCompleted.V6.Result
import io.quartic.eval.database.model.LegacyPhaseCompleted.V6.Result.Success
import org.flywaydb.core.api.MigrationVersion
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.isA
import org.jdbi.v3.core.Jdbi
import org.junit.*
import org.junit.Assert.assertThat
import org.junit.runners.MethodSorters
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DatabaseMigrationsShould {
    private val handle = DBI.open()

    @After
    fun after() {
        with(handle) {
            createUpdate("DELETE FROM build").execute()
            createUpdate("DELETE FROM event").execute()
            close()
        }
    }

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

        @Suppress("UNCHECKED_CAST")
        assertThat(readPayloadAs<V4>(eventId).result, isA(V4.Result.InternalError::class.java as Class<V4.Result>))
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

        assertThat(readPayloadAs<V5>(eventId).result, equalTo(
            V5.Result.UserError(
                V5.UserErrorInfo.OtherException(
                    mapOf("detail" to "wat")  // This is wrong - TODO - fix in v7
                )
            ) as V5.Result)
        )
        assertThatOtherEventsArentNuked(otherEventId)
    }

    @Test
    fun v6_migrate_execution_phases() {
        databaseVersion("5")


        val buildId = uuid(104)
        val phaseId = uuid(105)
        val time = Instant.now()

        val eventIdStart = uuid(103)
        val eventStart = insertEvent(eventIdStart, buildId, time,
            PhaseStarted(
                phaseId = phaseId,
                description = "Executing step for dataset foo"
            )
        )
        val eventIdLog = uuid(106)
        val eventLog = insertEvent(eventIdLog, buildId, time,
            LogMessageReceived(
                phaseId = phaseId,
                stream = "Yeah",
                message = "Right on"
            )
        )
        val eventIdComplete = uuid(107)
        insertEvent(eventIdComplete, buildId, time,
            V5(
                phaseId = phaseId,
                result = V5.Result.Success()
            )
        )

        databaseVersion("6")

        assertThat(readPayloadAs<V6>(eventIdComplete).result, equalTo(Success(NodeExecution(skipped = false)) as Result))
        // Check we didn't modify any other events
        assertThat(readPayloadAs<PhaseStarted>(eventIdStart), equalTo(eventStart))
        assertThat(readPayloadAs<LogMessageReceived>(eventIdLog), equalTo(eventLog))
    }

    @Test
    fun v6_not_affect_non_execution_phases() {
        databaseVersion("5")

        val eventIdStart = uuid(103)
        val buildId = uuid(104)
        val phaseId = uuid(105)
        val time = Instant.now()
        insertEvent(eventIdStart, buildId, time,
            PhaseStarted(
                phaseId = phaseId,
                description = "Something else"
            )
        )
        val eventIdComplete = uuid(106)
        insertEvent(eventIdComplete, buildId, time,
            V5(
                phaseId = phaseId,
                result = V5.Result.Success()
            )
        )

        databaseVersion("6")

        assertThat(readPayloadAs<V6>(eventIdComplete).result, equalTo(Success() as Result))
    }


    @Test
    fun v7_fix_bad_migration() {
        databaseVersion("6")
        val buildId = uuid(107)
        val phaseId = uuid(108)
        val badEventId = uuid(109)
        insertEvent(badEventId, buildId, Instant.now(),
            V6(
                phaseId = phaseId,
                result = V6.Result.UserError(
                    V5.UserErrorInfo.OtherException(V5.UserErrorInfo.OtherException("noob"))
                )
            )
        )

        val goodEventId = uuid(110)
        val goodEvent =
            V6(
                phaseId = phaseId,
                result = V6.Result.UserError(
                    V5.UserErrorInfo.OtherException("noob")
                )
            )
        insertEvent(goodEventId, UUID.randomUUID(), Instant.now(), goodEvent)

        val goodEventId2 = uuid(111)
        val goodEvent2 = V6(
            phaseId = UUID.randomUUID(),
            result = V6.Result.UserError(
                V5.UserErrorInfo.InvalidDag("noob dag", listOf())
            )
        )
        insertEvent(goodEventId2, UUID.randomUUID(), Instant.now(), goodEvent2)

        databaseVersion("7")
        val expected = Result.UserError(V5.UserErrorInfo.OtherException("noob"))

        assertThat(readPayloadAs<V6>(badEventId).result, equalTo(expected as V6.Result))
        assertThat(readPayloadAs<V6>(goodEventId).result, equalTo(goodEvent.result))
        assertThat(readPayloadAs<V6>(goodEventId2).result, equalTo(goodEvent2.result))
    }

    @Test
    @Suppress("unchecked")
    fun v8_migrate_node_structure() {
        databaseVersion("7")
        val buildId = uuid(112)
        val phaseId = uuid(113)
        val eventId = uuid(114)
        val nodes = listOf(
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
        )
        insertEvent(eventId, buildId, Instant.now(),
            V6(
                phaseId = phaseId,
                result = V6.Result.Success(
                    V6.Artifact.EvaluationOutput(nodes)
                )
            )
        )

        val errorEventId = uuid(115)
        insertEvent(errorEventId, buildId, Instant.now(),
            V6(
                phaseId = phaseId,
                result = V6.Result.UserError(
                    V5.UserErrorInfo.InvalidDag(
                        "noob dag",
                        nodes
                    )
                )
            )
        )

        databaseVersion("8")

        val payload = readPayloadAs<PhaseCompletedV8>(eventId)

        assertThat(payload.result, isA(PhaseCompletedV8.Result.Success::class.java) as Matcher<in PhaseCompletedV8.Result>)
        val result = payload.result as PhaseCompletedV8.Result.Success
        assertThat(result.artifact, isA(PhaseCompletedV8.Artifact.EvaluationOutput::class.java) as Matcher<in PhaseCompletedV8.Artifact?>)
        val artifact = result.artifact as PhaseCompletedV8.Artifact.EvaluationOutput
        assertThat(artifact.nodes.size, equalTo(3))

        artifact.nodes.forEach { node ->
            assertThat(node.name, equalTo(node.id))
        }

        val payloadError = readPayloadAs<PhaseCompletedV8>(errorEventId)

        assertThat(payloadError.result, isA(PhaseCompletedV8.Result.UserError::class.java) as Matcher<in PhaseCompletedV8.Result>)
        val resultError = payloadError.result as PhaseCompletedV8.Result.UserError
        assertThat(resultError.info, isA(PhaseCompletedV8.UserErrorInfo.InvalidDag::class.java) as Matcher<in PhaseCompletedV8.UserErrorInfo>)
    }

    private fun assertThatOtherEventsArentNuked(otherEventId: UUID) {
        assertThat(OBJECT_MAPPER.readValue(getEventFields(otherEventId)["payload"].toString()), isA(BuildSucceeded::class.java))
    }

    private inline fun <reified T : Any> readPayloadAs(eventId: UUID) =
        OBJECT_MAPPER.readValue<T>(getEventFields(eventId)["payload"].toString())

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

    private fun insertEvent(eventId: UUID, buildId: UUID, time: Instant, payload: Any): Any {
        handle.createUpdate("""
            INSERT INTO event (id, build_id, payload, time)
                VALUES (:id, :build_id, :payload, :time)
            """)
            .bind("id", eventId)
            .bind("build_id", buildId)
            .bindJson("payload", payload)
            .bind("time", time)
            .execute()
        return payload
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
