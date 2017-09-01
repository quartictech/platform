package io.quartic.eval.sequencer

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.registry.api.model.Customer
import java.time.Instant

interface Sequencer {
    suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit)

    interface SequenceBuilder {
        suspend fun phase(description: String, block: suspend PhaseBuilder.() -> Result)
    }

    interface PhaseBuilder {
        val container: QubeContainerProxy
        suspend fun log(stream: String, message: String, timestamp: Instant = Instant.now())
    }
}
