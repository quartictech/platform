package io.quartic.eval.apis

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.db.DatabaseBuilder
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import io.quartic.eval.apis.Database.EventType
import java.time.Instant
import org.hamcrest.MatcherAssert.*
import org.hamcrest.CoreMatchers.*
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import java.util.*

class DatabaseShould {
    @JvmField
    @Rule
    var pg = EmbeddedPostgresRules.singleInstance()

    private lateinit var database: Database

    @Before
    fun setUp() {
        database = DatabaseBuilder.testDao(Database::class.java, pg.embeddedPostgres.postgresDatabase)
    }

    @Test
    fun insert_build() {
        val id = UUID.randomUUID()
        val time = Instant.now()
        database.insertBuild(id, CustomerId(100L), triggerDetails, time)
        assertThat(database.getBuild(id), equalTo(
            Database.BuildRow(id, CustomerId(100), 1, triggerDetails, time)
        ))
    }

    @Test
    fun insert_phase() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        database.insertPhase(phaseId, buildId,"Thing", time)
    }

    @Test
    fun insert_event() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        database.insertEvent(UUID.randomUUID(), phaseId, EventType.MESSAGE, QuartyMessage.Log("stdout", "Hahaha"), time)
        database.insertEvent(UUID.randomUUID(), phaseId, EventType.MESSAGE, QuartyMessage.Result(steps), time)
    }

    @Test
    fun insert_terminal_event() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        database.insertTerminalEvent(UUID.randomUUID(), phaseId, EventType.MESSAGE, BuildResult.Success(steps), time)
        database.insertTerminalEvent(UUID.randomUUID(), phaseId, EventType.MESSAGE,
            BuildResult.UserError(mapOf("foo" to "bar")), time)
    }

    @Test
    fun get_latest_dag() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val time = Instant.now()
        database.insertBuild(buildId, CustomerId(100L), triggerDetails, time)
        database.insertPhase(phaseId, buildId,"Thing", time)
        database.insertTerminalEvent(eventId, phaseId, EventType.SUCCESS, BuildResult.Success(steps), time)
        val dag = database.getLatestDag(CustomerId(100L))
        assertThat(dag.message.dag, equalTo(steps))
    }

    @Test
    fun use_sequential_build_numbers_per_customer() {
        val customerA = CustomerId(100L)
        val customerB = CustomerId(101L)

        (1..10).forEach { count ->
            val idA = UUID.randomUUID()
            database.insertBuild(idA, customerA, triggerDetails, Instant.now())
            assertThat(database.getBuild(idA).buildNumber, equalTo(count.toLong()))
        }

        (1..5).forEach { count ->
            val idB = UUID.randomUUID()
            database.insertBuild(idB, customerB, triggerDetails, Instant.now())
            assertThat(database.getBuild(idB).buildNumber, equalTo(count.toLong()))
        }
    }

    @Test
    fun disallow_duplicate_build_ids() {
        val id = UUID.randomUUID()
        database.insertBuild(id, CustomerId(100), triggerDetails, Instant.now())

        assertThrows<UnableToExecuteStatementException> {
            database.insertBuild(id, CustomerId(100), triggerDetails, Instant.now())
        }
    }

    @Test
    fun serialize_build_result_version() {
        val buildResult = BuildResult.Success(steps)
        val json = OBJECT_MAPPER.writeValueAsString(buildResult)
        val buildResultDeser = OBJECT_MAPPER.readValue<Map<String, Any>>(json)

        assertThat(buildResultDeser.get("version") as Int, equalTo(BuildResult.VERSION))
    }

    val steps = listOf(
        Step(
            "something",
            "name",
            "a step",
            "something.py",
            listOf(0, 1000),
            listOf(Dataset("wat", "ds")),
            listOf(Dataset("some", "w"))
        )
    )

    val triggerDetails = TriggerDetails(
        type = "wat",
        deliveryId = "id",
        installationId = 100,
        repoId = 100,
        repoName = "repo",
        cloneUrl = URI.create("ref"),
        ref = "w",
        commit = "commit",
        timestamp = Instant.now()
    )
}
