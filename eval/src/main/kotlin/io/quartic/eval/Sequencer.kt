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
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import java.time.Instant
import java.util.*

class Sequencer(
    private val qube: QubeProxy,
    private val notifier: Notifier,
    private val database: Database,
    private val uuidGen: () -> UUID
) {
    private val threadPool = newFixedThreadPoolContext(2, "database")

    private class PhaseException : RuntimeException()

    interface SequenceBuilder {
        suspend fun phase(description: String, block: suspend PhaseBuilder.() -> Result)
    }

    interface PhaseBuilder {
        val phaseId: UUID
        val container: QubeContainerProxy
        suspend fun log(stream: String, message: String)
    }

    suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
        SequenceContext(details, customer).execute(block)
    }

    private inner class SequenceContext(
        private val details: TriggerDetails,
        private val customer: Customer
    ) {
        private val buildId = uuidGen()

        suspend fun execute(block: suspend SequenceBuilder.() -> Unit) {
            val build = insertBuild(customer.id, details)
            TriggerReceived(details).insert()

            val success: Boolean = try {
                qube.createContainer().use { container ->
                    ContainerAcquired(container.hostname).insert()

                    block(object : SequenceBuilder {
                        suspend override fun phase(description: String, block: suspend PhaseBuilder.() -> Result) {
                            val phaseId = uuidGen()
                            PhaseStarted(phaseId, description).insert()

                            val result = try {
                                block(object : PhaseBuilder {
                                    override val phaseId get() = phaseId
                                    override val container get() = container
                                    override suspend fun log(stream: String, message: String) {
                                        LogMessageReceived(phaseId, stream, message).insert()
                                    }

                                })
                            } catch (e: Exception) {
                                InternalError(e)
                            }

                            PhaseCompleted(phaseId, result).insert()

                            if (result !is Success) {
                                throw PhaseException()
                            }
                        }

                    })
                }
                true
            } catch (e: Exception) {
                false
            }
            (if (success) BuildEvent.BUILD_SUCCEEDED else BuildEvent.BUILD_FAILED).insert()
            notifier.notifyAbout(details, customer, build, success) // TODO - how about "failed on phase blah"?
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
}
