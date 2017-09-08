package io.quartic.eval.sequencer

import io.quartic.common.coroutines.use
import io.quartic.common.logging.logger
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
import io.quartic.eval.model.toTriggerReceived
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer.*
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
    private val LOG by logger()

    override suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(details, customer).execute(block)
    }

    private inner class SequenceContext(private val details: TriggerDetails, private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, details)
            insert(details.toTriggerReceived())
            notifier.notifyStart(details)

            val completionEvent = executeInContainer(block)
            insert(completionEvent)
            notifyComplete(build, completionEvent)
        }

        private suspend fun executeInContainer(block: suspend SequenceBuilder.() -> Unit) = try {
            qube.createContainer().use { container ->
                insert(ContainerAcquired(container.id, container.hostname))
                block(SequenceBuilderImpl(container))
            }
            BuildEvent.BUILD_SUCCEEDED
        } catch (pe: PhaseException) {
            BuildFailed(pe.message!!)
        } catch (e: Exception) {
            BuildFailed("Internal error")
        }

        private inner class SequenceBuilderImpl(private val container: QubeContainerProxy) : SequenceBuilder {
            suspend override fun <R> phase(description: String, block: suspend PhaseBuilder<R>.() -> PhaseResult<R>): R {
                val phaseId = uuidGen()
                insert(PhaseStarted(phaseId, description), phaseId)

                val result = try {
                    async(CommonPool) { block(PhaseBuilderImpl(phaseId, container)) }.use { blockAsync ->
                        select<PhaseResult<R>> {
                            blockAsync.onAwait { it }
                            container.errors.onReceive { throw it }
                        }
                    }
                } catch (e: Exception) {
                    PhaseResult.InternalError<R>(e)
                }

                insert(PhaseCompleted(phaseId, transformResult(result)), phaseId)
                return extractOutput(result)
            }
        }

        private fun transformResult(result: PhaseResult<*>) = when (result) {
            is PhaseResult.Success -> Result.Success()
            is PhaseResult.SuccessWithArtifact -> Result.Success(result.artifact)
            is PhaseResult.InternalError -> Result.InternalError(result.throwable)
            is PhaseResult.UserError -> Result.UserError(result.detail)
        }

        private fun <R> extractOutput(result: PhaseResult<R>) = when (result) {
            is PhaseResult.Success -> result.output
            is PhaseResult.SuccessWithArtifact -> result.output
            is PhaseResult.InternalError -> throw PhaseException("Internal error")
            is PhaseResult.UserError -> throw PhaseException(result.detail.toString())
        }

        private inner class PhaseBuilderImpl<R>(
            private val phaseId: UUID,
            override val container: QubeContainerProxy
        ) : PhaseBuilder<R> {
            suspend override fun log(stream: String, message: String, timestamp: Instant) {
                insert(LogMessageReceived(phaseId, stream, message), phaseId, timestamp)
            }
        }

        private suspend fun insert(
            event: BuildEvent,
            phaseId: UUID? = null,
            time: Instant = Instant.now()
        ) = run(threadPool) {
            LOG.info("Event: ${event}")
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
