package io.quartic.eval.sequencer

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.Notifier
import io.quartic.eval.Notifier.Event
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.BuildRow
import io.quartic.eval.database.model.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Artifact
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.OtherException
import io.quartic.eval.database.model.PhaseCompletedV6.Result.Success
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

class SequencerImplShould {

    @Test
    fun write_basic_sequence_to_db() = runBlocking {
        sequencer.sequence(details, customer) {}    // Do nothing

        val buildId = uuid(100)
        verify(database).insertBuild(buildId, CustomerId(999), "lovely")
        verify(database).insertEvent(eq(uuid(101)), eq(TriggerReceived(details.toDatabaseModel())), any(), eq(buildId))
        verify(database).insertEvent(eq(uuid(102)), eq(ContainerAcquired(uuid(9999), "a.b.c")), any(), eq(buildId))
        verify(database).insertEvent(eq(uuid(103)), eq(BUILD_SUCCEEDED), any(), eq(buildId))
    }

    @Test
    fun write_phase_sequence_to_db() = runBlocking {
        val artifact = mock<Artifact>()

        sequencer.sequence(details, customer) {
            phase("Yes") {
                successWithArtifact(artifact, Unit)
            } // Do nothing
        }

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseStarted(phaseId, "Yes")), any(), eq(buildId))
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, Success(artifact))), any(), eq(buildId))
    }

    @Test
    fun write_null_artifact_if_none_specified_in_phase_result() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("Yes") {
                success(Unit)
            }
        }

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseStarted(phaseId, "Yes")), any(), eq(buildId))
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, Success())), any(), eq(buildId))
    }

    @Test
    fun log_phase_messages() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("Yes") {
                log("foo", "bar")
                success(Unit)
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), any(), any())
    }

    @Test
    fun log_phase_messages_with_explicit_timestamps() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("Yes") {
                log("foo", "bar")
                success(Unit)
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), eq(Instant.EPOCH), any())
    }

    @Test
    fun fail_build_if_phase_fails() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("No") {
                internalError(mock())
            }
        }

        verify(database, never()).insertEvent(any(), eq(BUILD_SUCCEEDED), any(), any())
        verify(database).insertEvent(any(), eq(BuildFailed("Internal error")), any(), any())
    }

    @Test
    fun consider_phase_failed_if_it_throws_exception() = runBlocking {
        val exception = RuntimeException("Nooooo")

        sequencer.sequence(details, customer) {
            phase("No") {
                throw exception
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, INTERNAL_ERROR)), any(), any())
    }

    @Test
    fun handle_concurrent_container_failure() = runBlocking {
        val exception = QubeException("Stuff is bad")

        whenever(qubeContainer.errors).thenReturn(produce(CommonPool) {
            send(exception)
        })

        sequencer.sequence(details, customer) {
            phase("Yes") {
                delay(Long.MAX_VALUE)   // Effectively infinite
                success(Unit)
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, INTERNAL_ERROR)), any(), any())
    }

    @Test
    fun return_result_from_phase() = runBlocking {
        var x: Int? = null

        sequencer.sequence(details, customer) {
            x = phase("Yes") {
                success(37)
            }
        }

        assertThat(x, equalTo(37))
    }

    @Test
    fun provide_access_to_container() = runBlocking {
        var c: QubeContainerProxy? = null

        sequencer.sequence(details, customer) {
            c = container
        }

        assertThat(c, equalTo(qubeContainer))
    }

    @Test
    fun not_run_subsequent_phases_if_earlier_phase_fails() = runBlocking {
        var secondPhaseRun = false

        sequencer.sequence(details, customer) {
            phase<Unit>("First") {
                internalError(mock())
            }
            phase<Unit>("Second") {
                secondPhaseRun = true
                success(Unit)
            }
        }

        assertFalse(secondPhaseRun)
    }

    @Test
    fun send_start_notifications_before_any_real_workon_success() = runBlocking {
        sequencer.sequence(details, customer) {}    // Do nothing

        inOrder(notifier, qube) {
            runBlocking {
                verify(notifier).notifyQueue(details)
                verify(qube).createContainer()
                verify(notifier).notifyStart(details)
            }
        }
    }

    @Test
    fun notify_on_success() = runBlocking {
        sequencer.sequence(details, customer) {}    // Do nothing

        verify(notifier).notifyComplete(details, customer, 1234, Event.Success("Everything worked"))
    }

    @Test
    fun notify_on_internal_error() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("No") {
                internalError(mock())
            }
        }

        verify(notifier).notifyComplete(details, customer, 1234, Event.Failure("Internal error"))
    }

    @Test
    fun notify_on_user_error() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("No") { userError(OtherException("Bad things occurred")) }
        }

        verify(notifier).notifyComplete(details, customer, 1234, Event.Failure("Bad things occurred"))
    }

    private val details = BuildTrigger.GithubWebhook(
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoName = "noob",
        repoOwner = "noobing",
        ref = "refs/heads/lovely",
        commit = "abc123",
        timestamp = Instant.MIN,
        rawWebhook = emptyMap()
    )

    private val customer = mock<Customer> {
        on { id } doReturn CustomerId(999)
    }

    private val buildRow = mock<BuildRow> {
        on { buildNumber } doReturn 1234
    }

    private fun uuid(x: Int) = UUID(0, x.toLong())

    private var uuid = 100

    private val qubeContainer = mock<QubeContainerProxy> {
        on { id } doReturn uuid(9999)
        on { hostname } doReturn "a.b.c"
        on { errors } doReturn Channel()
    }

    private val qube = mock<QubeProxy> {
        on { runBlocking { createContainer() } } doReturn qubeContainer
    }

    private val database = mock<Database> {
        on { getBuild(any()) } doReturn buildRow
    }

    private val notifier = mock<Notifier>()

    private val clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())

    private val sequencer = SequencerImpl(qube, database, notifier, clock) { uuid(uuid++) }
}
