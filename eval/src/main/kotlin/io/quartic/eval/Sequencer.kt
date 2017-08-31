package io.quartic.eval

import io.quartic.common.coroutines.use
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.InternalError
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.selects.select
import java.time.Instant
import java.util.*

class Sequencer(
    private val qube: QubeProxy,
    private val database: Database,
    private val notifier: Notifier,
    private val uuidGen: () -> UUID = { UUID.randomUUID() }
) {
    suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(details, customer).execute(block)
    }

    interface SequenceBuilder {
        suspend fun phase(description: String, block: suspend PhaseBuilder.() -> Result)
    }

    interface PhaseBuilder {
        val phaseId: UUID
        val container: QubeContainerProxy
        suspend fun log(stream: String, message: String)
    }

    private inner class SequenceContext(private val details: TriggerDetails, private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, details)
            TriggerReceived(details).insert()

            val success = try {
                qube.createContainer().use { container ->
                    ContainerAcquired(container.hostname).insert()
                    block(SequenceBuilderImpl(container))
                }
                true
            } catch (e: Exception) {
                false
            }
            (if (success) BuildEvent.BUILD_SUCCEEDED else BuildEvent.BUILD_FAILED).insert()
            notifier.notifyAbout(details, customer, build, success) // TODO - how about "failed on phase blah"?
        }

        private inner class SequenceBuilderImpl(private val container: QubeContainerProxy) : SequenceBuilder {
            private val phaseId = uuidGen()

            suspend override fun phase(description: String, block: suspend PhaseBuilder.() -> Result) {
                PhaseStarted(phaseId, description).insert()

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

                PhaseCompleted(phaseId, result).insert()

                if (result !is Success) {
                    throw PhaseException()
                }
            }
        }

        private inner class PhaseBuilderImpl(
            override val phaseId: UUID,
            override val container: QubeContainerProxy
        ) : PhaseBuilder {
            suspend override fun log(stream: String, message: String) {
                LogMessageReceived(phaseId, stream, message).insert()
            }
        }

        private suspend fun BuildEvent.insert(time: Instant = Instant.now()) = run(threadPool) {
            database.insertEvent2(
                id = uuidGen(),
                buildId = buildId,
                time = time,
                payload = this
            )
        }

        private suspend fun insertBuild(customerId: CustomerId, trigger: TriggerDetails) = run(threadPool) {
            database.insertBuild(buildId, customerId, trigger.branch(), trigger)
            database.getBuild(buildId)
        }
    }

    private val threadPool = newFixedThreadPoolContext(2, "database")

    private class PhaseException : RuntimeException()
}
