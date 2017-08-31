package io.quartic.eval.sequencer

import io.quartic.common.coroutines.use
import io.quartic.common.model.CustomerId
import io.quartic.eval.Database
import io.quartic.eval.Notifier
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.SequenceBuilder
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
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
            BuildEvent.TriggerReceived(details).insert()

            val success = try {
                qube.createContainer().use { container ->
                    BuildEvent.ContainerAcquired(container.hostname).insert()
                    block(SequenceBuilderImpl(container))
                }
                true
            } catch (e: Exception) {
                false
            }
            (if (success) BuildEvent.BUILD_SUCCEEDED else BuildEvent.BUILD_FAILED).insert()
            notifier.notifyAbout(details, customer, build, success) // TODO - how about "failed on phase blah"?
        }

        private inner class SequenceBuilderImpl(private val container: QubeProxy.QubeContainerProxy) : SequenceBuilder {
            private val phaseId = uuidGen()

            suspend override fun phase(description: String, block: suspend PhaseBuilder.() -> Result) {
                BuildEvent.PhaseStarted(phaseId, description).insert()

                val result = try {
                    async(CommonPool) { block(PhaseBuilderImpl(phaseId, container)) }.use { blockAsync ->
                        select<Result> {
                            blockAsync.onAwait { it }
                            container.errors.onReceive { throw it }
                        }
                    }
                } catch (e: Exception) {
                    Result.InternalError(e)
                }

                BuildEvent.PhaseCompleted(phaseId, result).insert()

                if (result !is Success) {
                    throw PhaseException()
                }
            }
        }

        private inner class PhaseBuilderImpl(
            private val phaseId: UUID,
            override val container: QubeProxy.QubeContainerProxy
        ) : PhaseBuilder {
            suspend override fun log(stream: String, message: String) {
                BuildEvent.LogMessageReceived(phaseId, stream, message).insert()
            }
        }

        private suspend fun BuildEvent.insert(time: Instant = Instant.now()) = kotlinx.coroutines.experimental.run(threadPool) {
            database.insertEvent2(
                id = uuidGen(),
                buildId = buildId,
                time = time,
                payload = this
            )
        }

        private suspend fun insertBuild(customerId: CustomerId, trigger: TriggerDetails) = kotlinx.coroutines.experimental.run(threadPool) {
            database.insertBuild(buildId, customerId, trigger.branch())
            database.getBuild(buildId)
        }
    }

    private val threadPool = newFixedThreadPoolContext(2, "database")

    private class PhaseException : RuntimeException()
}
