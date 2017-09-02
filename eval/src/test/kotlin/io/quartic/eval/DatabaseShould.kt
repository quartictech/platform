package io.quartic.eval

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.UserError
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.Step
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.nullValue
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*

class DatabaseShould {
    private val customerId = customerId()
    private val branch = "develop"

    private val trigger = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoName = "noob",
        cloneUrl = URI("https://noob.com/foo/bar"),
        ref = "refs/heads/develop",
        commit = "abc123",
        timestamp = Instant.MIN
    )

    @Test
    fun insert_build() {
        val id = UUID.randomUUID()
        DATABASE.insertBuild(id, customerId, branch)
        assertThat(DATABASE.getBuild(id), equalTo(Database.BuildRow(id, customerId, branch, 1)))
    }

    @Test
    fun insert_event() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.BUILD_SUCCEEDED, time, buildId)
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.BUILD_SUCCEEDED, time, buildId, phaseId)
    }

    @Test
    fun get_latest_valid_dag() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), PhaseCompleted(phaseId, Success(EvaluationOutput(steps))), time, buildId, phaseId)
        val dag = DATABASE.getLatestValidDag(customerId)
        assertThat(dag!!.artifact.steps, equalTo(steps))
    }

    @Test
    fun ignore_failures_when_getting_latest_dag() {
        val buildIdA = UUID.randomUUID()
        val phaseIdA = UUID.randomUUID()
        DATABASE.insertBuild(buildIdA, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), PhaseCompleted(phaseIdA, Success(EvaluationOutput(steps))), Instant.now(), buildIdA, phaseIdA)

        val buildIdB = UUID.randomUUID()
        val phaseIdB = UUID.randomUUID()
        DATABASE.insertBuild(buildIdB, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), PhaseCompleted(phaseIdB, UserError("Noob")), Instant.now(), buildIdA, phaseIdB)

        val dag = DATABASE.getLatestValidDag(customerId)
        assertThat(dag!!.artifact.steps, equalTo(steps))
    }

    @Test
    fun fail_to_get_latest_if_nonexistent() {
        val buildId = UUID.randomUUID()
        DATABASE.insertBuild(buildId, customerId, branch)
        assertThat(DATABASE.getLatestValidDag(customerId), nullValue())
    }

    @Test
    fun get_valid_dag() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), PhaseCompleted(phaseId, Success(EvaluationOutput(steps))), time, buildId, phaseId)
        val dag = DATABASE.getValidDag(customerId, DATABASE.getBuild(buildId).buildNumber)
        assertThat(dag!!.artifact.steps, equalTo(steps))
    }

    @Test
    fun fail_to_get_valid_dag_if_not_actually_valid() {
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), PhaseCompleted(phaseId, UserError("Noob")), time, buildId, phaseId)
        val dag = DATABASE.getValidDag(customerId, DATABASE.getBuild(buildId).buildNumber)
        assertThat(dag, nullValue())
    }

    @Test
    fun use_sequential_build_numbers_per_customer() {
        val otherCustomerId = customerId()

        (1..10).forEach { count ->
            val idA = UUID.randomUUID()
            DATABASE.insertBuild(idA, customerId, branch)
            assertThat(DATABASE.getBuild(idA).buildNumber, equalTo(count.toLong()))
        }

        (1..5).forEach { count ->
            val idB = UUID.randomUUID()
            DATABASE.insertBuild(idB, otherCustomerId, branch)
            assertThat(DATABASE.getBuild(idB).buildNumber, equalTo(count.toLong()))
        }
    }

    @Test
    fun disallow_duplicate_build_ids() {
        val id = UUID.randomUUID()
        DATABASE.insertBuild(id, customerId, branch)

        assertThrows<UnableToExecuteStatementException> {
            DATABASE.insertBuild(id, customerId, branch)
        }
    }

    @Test
    fun get_builds() {
        val customerId = customerId()
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.TriggerReceived(trigger), time, buildId, phaseId)
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.BuildSucceeded(), time, buildId, phaseId)
        DBI.open().createQuery("select * from event")
            .mapToMap().forEach { println(it) }


        val builds = DATABASE.getBuilds(customerId)
        assertThat(builds.size, equalTo(1))
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
