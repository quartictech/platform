package io.quartic.eval.sequencer

import io.quartic.common.coroutines.use
import io.quartic.common.model.CustomerId
import io.quartic.eval.Database
import io.quartic.eval.Notifier
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.InternalError
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
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
    override suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(details, customer).execute(block)
    }

    private inner class SequenceContext(private val details: TriggerDetails, private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, details)
            insert(TriggerReceived(details))
            notifier.notifyStart(details)

            val success = try {
                qube.createContainer().use { container ->
                    insert(ContainerAcquired(container.hostname))
                    block(SequenceBuilderImpl(container))
                }
                true
            } catch (e: Exception) {
                false
            }
            insert((if (success) BuildEvent.BUILD_SUCCEEDED else BuildEvent.BUILD_FAILED))
            notifier.notifyComplete(details, customer, build.buildNumber, success) // TODO - how about "failed on phase blah"?
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
            is PhaseResult.Success -> Success()
            is PhaseResult.SuccessWithArtifact -> Success(result.artifact)
            is PhaseResult.InternalError -> InternalError(result.throwable)
            is PhaseResult.UserError -> Result.UserError(result.detail)
        }

        private fun <R> extractOutput(result: PhaseResult<R>) = when (result) {
            is PhaseResult.Success -> result.output
            is PhaseResult.SuccessWithArtifact -> result.output
            is PhaseResult.InternalError -> throw PhaseException()
            is PhaseResult.UserError -> throw PhaseException()
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
    }

    // TODO - we could run a single thread and send messages via a channel
    private val threadPool = newFixedThreadPoolContext(2, "database")

    private class PhaseException : RuntimeException()
}
