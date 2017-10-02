package io.quartic.eval.sequencer

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.Notifier
import io.quartic.eval.Notifier.Event
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.BuildRow
import io.quartic.eval.database.model.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.OtherException
import io.quartic.eval.database.model.PhaseCompletedV6.Artifact
import io.quartic.eval.database.model.PhaseCompletedV6.Result.Success
import io.quartic.qube.QubeProxy
import io.quartic.qube.QubeProxy.QubeContainerProxy
import io.quartic.qube.QubeProxy.QubeException
import io.quartic.qube.QubeProxy.QubeCompletion
import io.quartic.qube.api.model.PodSpec
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
        sequencer.sequence(buildContext) {}    // Do nothing

        verify(database).insertEvent(eq(uuid(100)), eq(ContainerAcquired(uuid(9999), "a.b.c")), any(), eq(buildId))
        verify(database).insertEvent(eq(uuid(101)), eq(BUILD_SUCCEEDED), any(), eq(buildId))
    }

    @Test
    fun write_phase_sequence_to_db() = runBlocking {
        val artifact = mock<Artifact>()

        sequencer.sequence(buildContext) {
            phase("Yes") {
                successWithArtifact(artifact, Unit)
            } // Do nothing
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(PhaseStarted(phaseId, "Yes")), any(), eq(buildId))
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, Success(artifact))), any(), eq(buildId))
    }

    @Test
    fun write_null_artifact_if_none_specified_in_phase_result() = runBlocking {
        sequencer.sequence(buildContext) {
            phase("Yes") {
                success(Unit)
            }
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(PhaseStarted(phaseId, "Yes")), any(), eq(buildId))
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, Success())), any(), eq(buildId))
    }

    @Test
    fun log_phase_messages() = runBlocking {
        sequencer.sequence(buildContext) {
            phase("Yes") {
                log("foo", "bar")
                success(Unit)
            }
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), any(), any())
    }

    @Test
    fun log_phase_messages_with_explicit_timestamps() = runBlocking {
        sequencer.sequence(buildContext) {
            phase("Yes") {
                log("foo", "bar")
                success(Unit)
            }
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), eq(Instant.EPOCH), any())
    }

    @Test
    fun fail_build_if_phase_fails() = runBlocking {
        sequencer.sequence(buildContext) {
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

        sequencer.sequence(buildContext) {
            phase("No") {
                throw exception
            }
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, INTERNAL_ERROR)), any(), any())
    }

    @Test
    fun handle_concurrent_container_failure() = runBlocking {
        val exception = QubeException("Stuff is bad")

        whenever(qubeContainer.completion).thenReturn(produce(CommonPool) {
            send(QubeCompletion.Exception(exception))
        })

        sequencer.sequence(buildContext) {
            phase("Yes") {
                delay(Long.MAX_VALUE)   // Effectively infinite
                success(Unit)
            }
        }

        val phaseId = uuid(101)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, INTERNAL_ERROR)), any(), any())
    }

    @Test
    fun return_result_from_phase() = runBlocking {
        var x: Int? = null

        sequencer.sequence(buildContext) {
            x = phase("Yes") {
                success(37)
            }
        }

        assertThat(x, equalTo(37))
    }

    @Test
    fun provide_access_to_container() = runBlocking {
        var c: QubeContainerProxy? = null

        sequencer.sequence(buildContext) {
            c = container
        }

        assertThat(c, equalTo(qubeContainer))
    }

    @Test
    fun not_run_subsequent_phases_if_earlier_phase_fails() = runBlocking {
        var secondPhaseRun = false

        sequencer.sequence(buildContext) {
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
    fun send_start_notifications_before_any_real_work() = runBlocking {
        sequencer.sequence(buildContext) {}    // Do nothing

        inOrder(notifier, qube) {
            runBlocking {
                verify(notifier).notifyQueue(details)
                verify(qube).createContainer(podSpec)
                verify(notifier).notifyStart(details)
            }
        }
    }

    @Test
    fun notify_on_success() = runBlocking {
        sequencer.sequence(buildContext) {}    // Do nothing

        verify(notifier).notifyComplete(details, customer, 1234, Event.Success("Everything worked"))
    }

    @Test
    fun notify_on_internal_error() = runBlocking {
        sequencer.sequence(buildContext) {
            phase("No") {
                internalError(mock())
            }
        }

        verify(notifier).notifyComplete(details, customer, 1234, Event.Failure("Internal error"))
    }

    @Test
    fun notify_on_user_error() = runBlocking {
        sequencer.sequence(buildContext) {
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

    private fun uuid(x: Int) = UUID(0, x.toLong())

    private var uuid = 100
    private val buildId = uuid(99)

    private val buildRow = mock<BuildRow> {
        on { id } doReturn buildId
        on { buildNumber } doReturn 1234
    }
    private val buildContext = BuildContext(details, customer, buildRow)

    private val qubeContainer = mock<QubeContainerProxy> {
        on { id } doReturn uuid(9999)
        on { hostname } doReturn "a.b.c"
        on { completion } doReturn Channel()
    }

    private val qube = mock<QubeProxy> {
        on { runBlocking { createContainer(any()) } } doReturn qubeContainer
    }

    private val database = mock<Database> {
        on { getBuild(any()) } doReturn buildRow
    }

    private val notifier = mock<Notifier>()

    private val clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())

    private val podSpec = mock<PodSpec>()
    private val sequencer = SequencerImpl(qube, database, notifier, podSpec, clock) { uuid(uuid++) }
}
