package io.quartic.eval.sequencer

import io.quartic.common.coroutines.use
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.Notifier
import io.quartic.eval.Notifier.Event.Failure
import io.quartic.eval.Notifier.Event.Success
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.BuildRow
import io.quartic.eval.database.model.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.InvalidDag
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.OtherException
import io.quartic.eval.database.model.PhaseCompletedV6.Result
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.selects.select
import java.time.Clock
import java.time.Instant
import java.util.*

class SequencerImpl(
    private val qube: QubeProxy,
    private val database: Database,
    private val notifier: Notifier,
    private val clock: Clock = Clock.systemUTC(),
    private val uuidGen: () -> UUID = { UUID.randomUUID() }
) : Sequencer {
    private val LOG by logger()

    override suspend fun sequence(trigger: BuildTrigger, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(trigger, customer).execute(block)
    }

    private inner class SequenceContext(private val trigger: BuildTrigger, private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, trigger)
            insert(TriggerReceived(trigger.toDatabaseModel()))
            notifier.notifyQueue(trigger)
            val completionEvent = executeInContainer(block)
            insert(completionEvent)
            notifyComplete(build, completionEvent)
        }

        private suspend fun executeInContainer(block: suspend SequenceBuilder.() -> Unit) = try {
            qube.createContainer().use { container ->
                insert(ContainerAcquired(container.id, container.hostname))
                notifier.notifyStart(trigger)
                block(SequenceBuilderImpl(container))
            }
            BUILD_SUCCEEDED
        } catch (pe: PhaseException) {
            BuildFailed(pe.message!!)
        } catch (e: Exception) {
            LOG.error("Build failed with internal error", e)
            BuildFailed("Internal error")
        }

        private inner class SequenceBuilderImpl(override val container: QubeContainerProxy) : SequenceBuilder {
            suspend override fun <R> phase(description: String, block: suspend PhaseBuilder<R>.() -> PhaseResult<R>): R {
                val phaseId = uuidGen()
                insert(PhaseStarted(phaseId, description))

                val result = try {
                    async(CommonPool) { block(PhaseBuilderImpl(phaseId)) }.use { blockAsync ->
                        select<PhaseResult<R>> {
                            blockAsync.onAwait { it }
                            container.errors.onReceive { throw it }
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("InternalError", e)
                    PhaseResult.InternalError<R>(e)
                }

                insert(PhaseCompleted(phaseId, transformResult(result)))
                return extractOutput(result)
            }
        }

        private fun transformResult(result: PhaseResult<*>) = when (result) {
            is PhaseResult.Success -> Result.Success()
            is PhaseResult.SuccessWithArtifact -> Result.Success(result.artifact)
            is PhaseResult.InternalError -> INTERNAL_ERROR
            is PhaseResult.UserError -> Result.UserError(result.info)
        }

        private fun <R> extractOutput(result: PhaseResult<R>) = when (result) {
            is PhaseResult.Success -> result.output
            is PhaseResult.SuccessWithArtifact -> result.output
            is PhaseResult.InternalError -> throw PhaseException("Internal error")
            is PhaseResult.UserError -> throw PhaseException(
                when (result.info) {
                    is InvalidDag -> result.info.error
                    is OtherException -> result.info.detail.toString()
                }
            )
        }


        private inner class PhaseBuilderImpl<R>(private val phaseId: UUID) : PhaseBuilder<R> {
            suspend override fun log(stream: String, message: String) {
                insert(LogMessageReceived(phaseId, stream, message), clock.instant())
            }
        }

        private suspend fun insert(
            event: BuildEvent,
            time: Instant = Instant.now()
        ) = run(threadPool) {
            LOG.info("Event: ${event}")
            database.insertEvent(
                id = uuidGen(),
                payload = event,
                time = time,
                buildId = buildId
            )
        }

        private suspend fun insertBuild(customerId: CustomerId, trigger: BuildTrigger) = run(threadPool) {
            database.insertBuild(buildId, customerId, trigger.branch())
            database.getBuild(buildId)
        }

        private suspend fun notifyComplete(build: BuildRow, completionEvent: BuildCompleted) {
            notifier.notifyComplete(
                trigger,
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
