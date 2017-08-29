package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.bindJson
import io.quartic.common.db.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.eval.Database.EventType.MESSAGE
import io.quartic.eval.Database.EventType.SUCCESS
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildResult
import io.quartic.eval.model.BuildResult.Success
import io.quartic.eval.model.BuildResult.UserError
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.QuartyMessage.Log
import io.quartic.quarty.model.QuartyMessage.Result
import io.quartic.quarty.model.Step
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.nullValue
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.postgresql.util.PGobject
import java.net.URI
import java.time.Instant
import java.util.*

class DatabaseShould {
    private val customerId = customerId()
    val branch = "develop"

    @Test
    fun insert_build() {
        val id = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(id, customerId, branch, triggerDetails, time)
        assertThat(DATABASE.getBuild(id), equalTo(Database.BuildRow(id, customerId, branch,
            1, triggerDetails, time)))
    }

    @Test
    fun insert_phase() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertPhase(phaseId, buildId,"Thing", time)
    }

    @Test
    fun insert_event() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertEvent(UUID.randomUUID(), phaseId, MESSAGE, Log("stdout", "Hahaha"), time)
        DATABASE.insertEvent(UUID.randomUUID(), phaseId, MESSAGE, Result(steps), time)
    }

    @Test
    fun insert_terminal_event() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertTerminalEvent(UUID.randomUUID(), phaseId, MESSAGE, Success(steps), time)
        DATABASE.insertTerminalEvent(UUID.randomUUID(), phaseId, MESSAGE, UserError(mapOf("foo" to "bar")), time)
    }

    @Test
    fun get_latest_success() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch, triggerDetails, time)
        DATABASE.insertPhase(phaseId, buildId,"Thing", time)
        DATABASE.insertTerminalEvent(eventId, phaseId, SUCCESS, Success(steps), time)
        val dag = DATABASE.getLatestSuccess(customerId)
        assertThat(dag!!.message.steps, equalTo(steps))
    }

    @Test
    fun get_latest_success_fails_on_nonexistent() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch, triggerDetails, time)
        DATABASE.insertPhase(phaseId, buildId,"Thing", time)
        assertThat(DATABASE.getLatestSuccess(customerId), nullValue())
    }

    @Test
    fun use_sequential_build_numbers_per_customer() {
        val otherCustomerId = customerId()

        (1..10).forEach { count ->
            val idA = UUID.randomUUID()
            DATABASE.insertBuild(idA, customerId, branch, triggerDetails, Instant.now())
            assertThat(DATABASE.getBuild(idA).buildNumber, equalTo(count.toLong()))
        }

        (1..5).forEach { count ->
            val idB = UUID.randomUUID()
            DATABASE.insertBuild(idB, otherCustomerId, branch, triggerDetails, Instant.now())
            assertThat(DATABASE.getBuild(idB).buildNumber, equalTo(count.toLong()))
        }
    }


    @Test
    fun disallow_duplicate_build_ids() {
        val id = UUID.randomUUID()
        DATABASE.insertBuild(id, customerId, branch, triggerDetails, Instant.now())

        assertThrows<UnableToExecuteStatementException> {
            DATABASE.insertBuild(id, customerId, branch, triggerDetails, Instant.now())
        }
    }

    @Test
    fun serialize_build_result_version() {
        val buildResult = Success(steps)
        val json = OBJECT_MAPPER.writeValueAsString(buildResult)
        val buildResultDeser = OBJECT_MAPPER.readValue<Map<String, Any>>(json)

        assertThat(buildResultDeser.get("version") as Int, equalTo(BuildResult.VERSION))
    }

    @Test
    fun check_build_result_version() {
        val buildResult = Success(steps)

        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch, triggerDetails, time)
        DATABASE.insertPhase(phaseId, buildId, "noob", time)
        DATABASE.insertTerminalEvent(UUID.randomUUID(), phaseId, SUCCESS, buildResult, time)

        // Tweak the JSON to change the version
        val json = OBJECT_MAPPER.writeValueAsString(buildResult)
        val buildResultFudge = OBJECT_MAPPER.readValue<MutableMap<String, Any>>(json)
        buildResultFudge.set("version", -100)
        DBI.open().createUpdate("update event set message = :message")
            .bindJson("message", buildResultFudge)
            .execute()

        assertThrows<IllegalStateException> {
            DATABASE.getLatestSuccess(customerId)
        }
    }

    private fun customerId() = CustomerId(Random().nextLong())

    private val steps = listOf(
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

    private val triggerDetails = TriggerDetails(
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

    companion object {
        @ClassRule
        @JvmField
        val PG = EmbeddedPostgresRules.singleInstance()

        private lateinit var DATABASE: Database
        private lateinit var DBI: Jdbi

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DATABASE = DatabaseBuilder.testDao(Database::class.java, PG.embeddedPostgres.postgresDatabase)
            DBI = setupDbi(Jdbi.create(PG.embeddedPostgres.postgresDatabase))
        }
    }
}
