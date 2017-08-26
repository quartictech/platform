package io.quartic.eval.apis

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.model.CustomerId
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
            Database.Build(id, CustomerId(100), 1, triggerDetails, time)
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
    fun insert_message() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        database.insertMessage(UUID.randomUUID(), phaseId, "SomeThing", QuartyMessage.Log("stdout", "Hahaha"), time)
        database.insertMessage(UUID.randomUUID(), phaseId, "SomeThing", QuartyMessage.Result(steps), time)
    }

    @Test
    fun insert_terminal_message() {
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        database.insertTerminalMessage(UUID.randomUUID(), phaseId, "SomeThing", Database.BuildResult.Success(steps), time)
        database.insertTerminalMessage(UUID.randomUUID(), phaseId, "SomeThing",
            Database.BuildResult.UserError(mapOf("foo" to "bar")), time)
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

    val triggerDetails = TriggerDetails("wat", "id", 100, 100, URI.create("git"), "ref", "w", Instant.now())
}
