package io.quartic.eval.sequencer

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.Database
import io.quartic.eval.Database.BuildRow
import io.quartic.eval.Notifier
import io.quartic.eval.Notifier.Event
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.BuildCompleted.BuildFailed
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
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
import java.time.Instant
import java.util.*

class SequencerImplShould {

    @Test
    fun write_basic_sequence_to_db() = runBlocking {
        sequencer.sequence(details, customer) {}    // Do nothing

        val buildId = uuid(100)
        verify(database).insertBuild(buildId, CustomerId(999), "lovely")
        verify(database).insertEvent(eq(uuid(101)), eq(TriggerReceived(details)), any(), eq(buildId), eq(null))
        verify(database).insertEvent(eq(uuid(102)), eq(ContainerAcquired("a.b.c")), any(), eq(buildId), eq(null))
        verify(database).insertEvent(eq(uuid(103)), eq(BuildEvent.BUILD_SUCCEEDED), any(), eq(buildId), eq(null))
    }

    @Test
    fun write_phase_sequence_to_db() = runBlocking {
        val result = Success(mock())

        sequencer.sequence(details, customer) {
            phase("Yes") { result } // Do nothing
        }

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseStarted(phaseId, "Yes")), any(), eq(buildId), eq(phaseId))
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, result)), any(), eq(buildId), eq(phaseId))
    }

    @Test
    fun log_phase_messages() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("Yes") {
                log("foo", "bar")
                Success(mock())  // Irrelevant for this test
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), any(), any(), eq(phaseId))
    }

    @Test
    fun log_phase_messages_with_explicit_timestamps() = runBlocking {
        val instant = Instant.EPOCH

        sequencer.sequence(details, customer) {
            phase("Yes") {
                log("foo", "bar", instant)
                Success(mock())  // Irrelevant for this test
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(LogMessageReceived(phaseId, "foo", "bar")), eq(instant), any(), eq(phaseId))
    }

    @Test
    fun fail_build_if_phase_fails() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("No") { InternalError(mock()) }
        }

        verify(database, never()).insertEvent(any(), eq(BuildEvent.BUILD_SUCCEEDED), any(), any(), eq(null))
        verify(database).insertEvent(any(), eq(BuildFailed("Internal error")), any(), any(), eq(null))
    }

    @Test
    fun consider_phase_failed_if_it_throws_exception() = runBlocking {
        val exception = RuntimeException("Nooooo")

        sequencer.sequence(details, customer) {
            phase("No") { throw exception }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, InternalError(exception))), any(), any(), eq(phaseId))
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
                Success(mock())
            }
        }

        val phaseId = uuid(103)
        verify(database).insertEvent(any(), eq(PhaseCompleted(phaseId, InternalError(exception))), any(), any(), eq(phaseId))
    }

    @Test
    fun provide_access_to_container_during_all_phases() = runBlocking {
        var c1: QubeContainerProxy? = null
        var c2: QubeContainerProxy? = null

        sequencer.sequence(details, customer) {
            phase("First") {
                c1 = container
                Success(mock())
            }
            phase("Second") {
                c2 = container
                Success(mock())
            }
        }

        assertThat(c1, equalTo(qubeContainer))
        assertThat(c2, equalTo(qubeContainer))
    }

    @Test
    fun not_run_subsequent_phases_if_earlier_phase_fails() = runBlocking {
        var secondPhaseRun = false

        sequencer.sequence(details, customer) {
            phase("First") { InternalError(mock()) }
            phase("Second") {
                secondPhaseRun = true
                Success(mock())
            }
        }

        assertFalse(secondPhaseRun)
    }

    @Test
    fun send_start_notification_before_any_real_workon_success() = runBlocking {
        sequencer.sequence(details, customer) {}    // Do nothing

        inOrder(notifier, qube) {
            runBlocking {
                verify(notifier).notifyStart(details)
                verify(qube).createContainer()
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
            phase("No") { InternalError(mock()) }
        }

        verify(notifier).notifyComplete(details, customer, 1234, Event.Failure("Internal error"))
    }

    @Test
    fun notify_on_user_error() = runBlocking {
        sequencer.sequence(details, customer) {
            phase("No") { UserError("Bad things occurred") }
        }

        verify(notifier).notifyComplete(details, customer, 1234, Event.Failure("Bad things occurred"))
    }

    private val details = mock<TriggerDetails> {
        on { branch() } doReturn "lovely"
    }

    private val customer = mock<Customer> {
        on { id } doReturn CustomerId(999)
    }

    private val buildRow = mock<BuildRow> {
        on { buildNumber } doReturn 1234
    }

    private fun uuid(x: Int) = UUID(0, x.toLong())

    private var uuid = 100

    private val qubeContainer = mock<QubeContainerProxy> {
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

    private val sequencer = SequencerImpl(qube, database, notifier) { uuid(uuid++) }
}