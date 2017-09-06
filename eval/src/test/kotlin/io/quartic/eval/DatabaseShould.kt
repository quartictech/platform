package io.quartic.eval

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.db.setupDbi
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.Database.BuildRow
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.toTriggerReceived
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.Step
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.nullValue
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.After
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import java.net.URI
import java.time.Duration
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
        repoFullName = "noobing/noob",
        repoOwner = "noobing",
        cloneUrl = URI("https://noob.com/foo/bar"),
        ref = "refs/heads/develop",
        commit = "abc123",
        timestamp = Instant.MIN
    )
    private val uuidGen = UuidGen()
    private val buildId = uuidGen()
    private val phaseId = uuidGen()

    @After
    fun after() {
        DBI.open().createUpdate("DELETE FROM build").execute()
        DBI.open().createUpdate("DELETE FROM event").execute()
    }

    @Test
    fun insert_build() {
        insertBuild(buildId)

        assertThat(DATABASE.getBuild(buildId), equalTo(BuildRow(buildId, customerId, branch, 1)))
    }

    @Test
    fun insert_event() {
        insertBuild(buildId)
        insertEvent(buildId, phaseId, successfulPhase(phaseId))

        assertThat(DATABASE.getEventsForBuild(customerId, 1).map { it.payload }, contains(
            successfulPhase(phaseId) as BuildEvent
        ))
    }

    @Test
    fun get_events_for_build_in_chronological_order() {
        val time = Instant.now()
        insertBuild(buildId)
        insertEvent(buildId, phaseId, successfulPhase(uuid(69)), time)
        insertEvent(buildId, phaseId, successfulPhase(uuid(70)), time - Duration.ofSeconds(1))
        insertEvent(buildId, phaseId, successfulPhase(uuid(71)), time + Duration.ofSeconds(1))
        insertEvent(buildId, phaseId, successfulPhase(uuid(72)), time - Duration.ofSeconds(2))

        assertThat(DATABASE.getEventsForBuild(customerId, 1).map { it.payload }, contains(
            successfulPhase(uuid(72)) as BuildEvent,
            successfulPhase(uuid(70)) as BuildEvent,
            successfulPhase(uuid(69)) as BuildEvent,
            successfulPhase(uuid(71)) as BuildEvent
        ))
    }

    @Test
    fun get_events_for_only_specified_build() {
        insertBuild(buildId)
        insertEvent(buildId, phaseId, successfulPhase(uuid(42)))

        val otherBuildId = uuidGen()
        insertBuild(otherBuildId)
        insertEvent(otherBuildId, phaseId, successfulPhase(uuid(69)))

        assertThat(DATABASE.getEventsForBuild(customerId, 1).map { it.payload }, contains(
            successfulPhase(uuid(42)) as BuildEvent
        ))
    }

    @Test
    fun get_latest_successful_build_number() {
        // Build #1
        insertBuild(buildId)
        insertEvent(buildId, phaseId, BuildEvent.BUILD_SUCCEEDED)
        // Build #2
        insertBuild(uuid(1000))
        insertEvent(uuid(1000), phaseId, BuildEvent.BUILD_SUCCEEDED)
        // Build #3
        insertBuild(uuid(2000))
        insertEvent(uuid(2000), phaseId, BuildEvent.BUILD_CANCELLED)

        assertThat(DATABASE.getLatestSuccessfulBuildNumber(customerId), equalTo(2L))
    }

    @Test
    fun return_null_if_no_successful_builds_for_customer() {
        assertThat(DATABASE.getLatestSuccessfulBuildNumber(customerId), nullValue())
    }

    @Test
    fun use_sequential_build_numbers_per_customer() {
        val otherCustomerId = customerId()

        (1..10).forEach { count ->
            val idA = uuidGen()
            insertBuild(idA)

            assertThat(DATABASE.getBuild(idA).buildNumber, equalTo(count.toLong()))
        }

        (1..5).forEach { count ->
            val idB = uuidGen()
            insertBuild(idB, otherCustomerId)

            assertThat(DATABASE.getBuild(idB).buildNumber, equalTo(count.toLong()))
        }
    }

    @Test
    fun disallow_duplicate_build_ids() {
        insertBuild(buildId)

        assertThrows<UnableToExecuteStatementException> {
            insertBuild(buildId)
        }
    }

    @Test
    fun get_builds_succeeded() {
        val customerId = customerId()
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), trigger.toTriggerReceived(), time, buildId, phaseId)
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.BUILD_SUCCEEDED, time, buildId, phaseId)

        val builds = DATABASE.getBuilds(customerId)
        assertThat(builds.size, equalTo(1))
        assertThat(builds[0].status, equalTo("success"))
    }

    @Test
    fun get_builds_failed() {
        val customerId = customerId()
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), trigger.toTriggerReceived(), time, buildId, phaseId)
        DATABASE.insertEvent(UUID.randomUUID(), BuildEvent.BuildCompleted.BuildFailed("noob"), time, buildId, phaseId)

        val builds = DATABASE.getBuilds(customerId)
        assertThat(builds.size, equalTo(1))
        assertThat(builds[0].status, equalTo("failure"))
    }

    @Test
    fun get_builds_running() {
        val customerId = customerId()
        val buildId = UUID.randomUUID()
        val phaseId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildId, customerId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), trigger.toTriggerReceived(), time, buildId, phaseId)
        val builds = DATABASE.getBuilds(customerId)

        assertThat(builds.size, equalTo(1))
    }

    @Test
    fun get_builds_only_for_customer() {
        val customerAId = customerId()
        val customerBId = customerId()
        val buildAId = UUID.randomUUID()
        val buildBId = UUID.randomUUID()
        val time = Instant.now()
        DATABASE.insertBuild(buildAId, customerAId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), trigger.toTriggerReceived(), time, buildAId, UUID.randomUUID())

        DATABASE.insertBuild(buildBId, customerBId, branch)
        DATABASE.insertEvent(UUID.randomUUID(), trigger.toTriggerReceived(), time, buildBId, UUID.randomUUID())

        val builds = DATABASE.getBuilds(customerAId)
        assertThat(builds.size, equalTo(1))
    }

    @Test
    fun filter_builds_without_trigger() {
        val customerId = customerId()
        val buildId = UUID.randomUUID()
        DATABASE.insertBuild(buildId, customerId, branch)
        val builds = DATABASE.getBuilds(customerId)
        assertThat(builds.size, equalTo(0))
    }

    private fun insertBuild(buildId: UUID, customerId: CustomerId = this.customerId) {
        DATABASE.insertBuild(buildId, customerId, branch)
    }

    private fun insertEvent(buildId: UUID, phaseId: UUID? = null, event: BuildEvent, time: Instant = Instant.now()) {
        DATABASE.insertEvent(uuidGen(), event, time, buildId, phaseId)
    }

    private inner class UuidGen {
        private var next = 100
        operator fun invoke(): UUID = uuid(next++)
    }

    private fun uuid(x: Int) = UUID(0, x.toLong())

    private fun customerId() = CustomerId(Random().nextLong())

    private fun successfulPhase(phaseIdA: UUID) = PhaseCompleted(phaseIdA, Success(EvaluationOutput(steps)))

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
