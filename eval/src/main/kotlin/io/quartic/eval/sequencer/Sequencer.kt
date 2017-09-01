package io.quartic.eval.sequencer

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.registry.api.model.Customer
import java.time.Instant

interface Sequencer {
    suspend fun sequence(details: TriggerDetails, customer: Customer, block: suspend SequenceBuilder.() -> Unit)

    interface SequenceBuilder {
        suspend fun <R> phase(description: String, block: suspend PhaseBuilder<R>.() -> PhaseResult<R>): R
    }

    interface PhaseBuilder<R> {
        val container: QubeContainerProxy
        suspend fun log(stream: String, message: String, timestamp: Instant = Instant.now())

        // Helpers
//        fun success() = success(Unit)
        fun success(output: R) = PhaseResult.Success(output)
//        fun successWithArtifact(artifact: Artifact) = successWithArtifact(artifact, Unit)
        fun successWithArtifact(artifact: Artifact, output: R) = PhaseResult.SuccessWithArtifact(artifact, output)
        fun internalError(throwable: Throwable) = PhaseResult.InternalError<R>(throwable)
        fun userError(detail: Any?) = PhaseResult.UserError<R>(detail)
    }

    sealed class PhaseResult<out R> {
        data class Success<out R>(val output: R) : PhaseResult<R>()
        data class SuccessWithArtifact<out R>(val artifact: Artifact, val output: R) : PhaseResult<R>()
        data class InternalError<out R>(val throwable: Throwable) : PhaseResult<R>()
        data class UserError<out R>(val detail: Any?) : PhaseResult<R>()
    }
}
