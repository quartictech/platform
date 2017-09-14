package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.InternalError
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.UserError
import io.quartic.eval.sequencer.Sequencer.PhaseResult.Success
import io.quartic.quarty.api.model.Step
import java.util.*

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(TriggerReceived::class, name = "trigger_received"),
    Type(BuildCancelled::class, name = "build_cancelled"),
    Type(BuildSucceeded::class, name = "build_succeeded"),
    Type(BuildFailed::class, name = "build_failed"),
    Type(ContainerAcquired::class, name = "container_acquired"),
    Type(PhaseStarted::class, name = "phase_started"),
    Type(PhaseCompleted::class, name = "phase_completed"),
    Type(LogMessageReceived::class, name = "log_message_received")
)
sealed class BuildEvent

/**
 * Current event definitions
 */
data class CurrentTriggerReceived(val trigger: BuildTrigger) : BuildEvent()
sealed class BuildCompleted : BuildEvent()
class CurrentBuildCancelled : BuildCompleted()
class CurrentBuildSucceeded : BuildCompleted()
data class CurrentBuildFailed(val description: String) : BuildCompleted()
data class CurrentContainerAcquired(val containerId: UUID, val hostname: String) : BuildEvent()
data class CurrentPhaseStarted(val phaseId: UUID, val description: String) : BuildEvent()
data class CurrentPhaseCompleted(val phaseId: UUID, val result: Result) : BuildEvent() {
    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Success::class, name = "success"),
        Type(InternalError::class, name = "internal_error"),
        Type(UserError::class, name = "user_error")
    )
    sealed class Result {
        data class Success(val artifact: Artifact? = null) : Result()
        data class InternalError(val throwable: Throwable) : Result()
        data class UserError(val detail: Any?) : Result()
    }

    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(EvaluationOutput::class, name = "evaluation_output")
    )
    sealed class Artifact {
        // TODO - have our own Step to decouple DB schema
        data class EvaluationOutput(val steps: List<Step>) : Artifact()
    }
}
data class CurrentLogMessageReceived(val phaseId: UUID, val stream: String, val message: String) : BuildEvent()

/**
 * Typealiases to decouple codebase from version refactorings
 */
typealias TriggerReceived = CurrentTriggerReceived
typealias BuildCancelled = CurrentBuildCancelled
typealias BuildSucceeded = CurrentBuildSucceeded
typealias BuildFailed = CurrentBuildFailed
typealias ContainerAcquired = CurrentContainerAcquired
typealias PhaseStarted = CurrentPhaseStarted
typealias PhaseCompleted = CurrentPhaseCompleted
typealias LogMessageReceived = CurrentLogMessageReceived

// To make testing easier given one can't have zero-arg data classes
val BUILD_CANCELLED = BuildCancelled()
val BUILD_SUCCEEDED = BuildSucceeded()
