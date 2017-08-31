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
            notifier.notifyAbout(details, customer, build.buildNumber, success) // TODO - how about "failed on phase blah"?
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

                if (result !is Success) {
                    throw PhaseException()
                }
            }
        }

        private inner class PhaseBuilderImpl(
            private val phaseId: UUID,
            override val container: QubeContainerProxy
        ) : PhaseBuilder {
            suspend override fun log(stream: String, message: String) {
                insert(LogMessageReceived(phaseId, stream, message), phaseId)
            }
        }

        private suspend fun insert(event: BuildEvent, phaseId: UUID? = null) = run(threadPool) {
            database.insertEvent(
                id = uuidGen(),
                payload = event,
                time = Instant.now(),
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
