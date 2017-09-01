package io.quartic.eval.sequencer

import io.quartic.common.coroutines.use
import io.quartic.common.model.CustomerId
import io.quartic.eval.Database
import io.quartic.eval.Database.BuildRow
import io.quartic.eval.Notifier
import io.quartic.eval.Notifier.Event.Failure
import io.quartic.eval.Notifier.Event.Success
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.BuildCompleted.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.InternalError
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.UserError
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.SequenceBuilder
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.selects.select
import java.time.Instant
import java.util.*

class SequencerImpl(
    private val qube: QubeProxy,
    private val database: Database,
    private val notifier: Notifier,
    private val uuidGen: () -> UUID = { UUID.randomUUID() }
) : Sequencer {
    override suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(details, customer).execute(block)
    }

    private inner class SequenceContext(private val details: TriggerDetails, private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, details)
            insert(TriggerReceived(details))
            notifier.notifyStart(details)

            val completionEvent = executeInContainer(block)
            insert(completionEvent)
            notifyComplete(build, completionEvent)
        }

        private suspend fun executeInContainer(block: suspend SequenceBuilder.() -> Unit) = try {
            qube.createContainer().use { container ->
                insert(ContainerAcquired(container.hostname))
                block(SequenceBuilderImpl(container))
            }
            BuildEvent.BUILD_SUCCEEDED
        } catch (pe: PhaseException) {
            BuildFailed(pe.message!!)
        } catch (e: Exception) {
            BuildFailed("Internal error")
        }

        private inner class SequenceBuilderImpl(private val container: QubeContainerProxy) : SequenceBuilder {
            suspend override fun phase(description: String, block: suspend PhaseBuilder.() -> Result) {
                val phaseId = uuidGen()
                insert(PhaseStarted(phaseId, description), phaseId)

                val result = try {
                    async(CommonPool) { block(PhaseBuilderImpl(phaseId, container)) }.use { blockAsync ->
                        select<Result> {
                            blockAsync.onAwait { it }
                            container.errors.onReceive { throw it }
                        }
                    }
                } catch (e: Exception) {
                    InternalError(e)
                }

                insert(PhaseCompleted(phaseId, result), phaseId)
                throwIfUnsuccessful(result)
            }

            private fun throwIfUnsuccessful(result: Result) {
                when (result) {
                    is UserError -> throw PhaseException(result.detail.toString())
                    is InternalError -> throw PhaseException("Internal error")
                    else -> {}  // Do nothing
                }
            }
        }

        private inner class PhaseBuilderImpl(
            private val phaseId: UUID,
            override val container: QubeContainerProxy
        ) : PhaseBuilder {
            suspend override fun log(stream: String, message: String, timestamp: Instant) {
                insert(LogMessageReceived(phaseId, stream, message), phaseId, timestamp)
            }
        }

        private suspend fun insert(
            event: BuildEvent,
            phaseId: UUID? = null,
            time: Instant = Instant.now()
        ) = run(threadPool) {
            database.insertEvent(
                id = uuidGen(),
                payload = event,
                time = time,
                buildId = buildId,
                phaseId = phaseId
            )
        }

        private suspend fun insertBuild(customerId: CustomerId, details: TriggerDetails) = run(threadPool) {
            database.insertBuild(buildId, customerId, details.branch())
            database.getBuild(buildId)
        }

        private suspend fun notifyComplete(build: BuildRow, completionEvent: BuildCompleted) {
            notifier.notifyComplete(
                details,
                customer,
                build.buildNumber,
                when (completionEvent) {
                    is BuildSucceeded -> Success("Everything worked")
                    is BuildFailed -> Failure(completionEvent.description)
                    is BuildCancelled -> Failure("Cancelled") // TODO - we're not using this yet
                }
            )
        }
    }

    // TODO - we could run a single thread and send messages via a channel
    private val threadPool = newFixedThreadPoolContext(2, "database")

    private class PhaseException(message: String) : RuntimeException(message)
}
