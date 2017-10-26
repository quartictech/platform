package io.quartic.eval.sequencer

import io.quartic.eval.database.model.PhaseCompletedV7.UserErrorInfo
import io.quartic.eval.database.model.PhaseCompletedV7.Artifact
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer.PhaseResult.*

interface Sequencer {
    suspend fun sequence(context: BuildInitiator.BuildContext, block: suspend SequenceBuilder.() -> Unit)

    interface SequenceBuilder {
        val container: QubeContainerProxy
        suspend fun <R> phase(description: String, block: suspend PhaseBuilder<R>.() -> PhaseResult<R>): R
    }

    interface PhaseBuilder<R> {
        suspend fun log(stream: String, message: String)

        // Helpers
        fun success(output: R): PhaseResult<R> = Success(output)
        fun successWithArtifact(artifact: Artifact, output: R): PhaseResult<R> = SuccessWithArtifact(artifact, output)
        fun internalError(throwable: Throwable): PhaseResult<R> = InternalError(throwable)
        fun userError(info: UserErrorInfo): PhaseResult<R> = UserError(info)
    }

    sealed class PhaseResult<out R> {
        data class Success<out R>(val output: R) : PhaseResult<R>()
        data class SuccessWithArtifact<out R>(val artifact: Artifact, val output: R) : PhaseResult<R>()
        data class InternalError<out R>(val throwable: Throwable) : PhaseResult<R>()
        data class UserError<out R>(val info: UserErrorInfo): PhaseResult<R>()
    }
}
