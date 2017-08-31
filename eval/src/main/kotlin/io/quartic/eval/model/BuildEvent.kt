package io.quartic.eval.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.quarty.model.Step
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
sealed class BuildEvent {
    // TODO - have our own TriggerDetails to decouple DB schema
    data class TriggerReceived(val details: TriggerDetails) : BuildEvent()

    class BuildCancelled : BuildEvent()

    class BuildSucceeded : BuildEvent()

    class BuildFailed : BuildEvent()

    data class ContainerAcquired(val hostname: String) : BuildEvent()

    data class PhaseStarted(val phaseId: UUID, val description: String) : BuildEvent()

    data class PhaseCompleted(val phaseId: UUID, val result: Result) : BuildEvent() {

        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(Success::class, name = "success"),
            Type(InternalError::class, name = "internal_error"),
            Type(UserError::class, name = "user_error")
        )
        sealed class Result {
            data class Success(val artifact: Artifact) : Result() {

                @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
                @JsonSubTypes(
                    Type(EvaluationOutput::class, name = "evaluation_output")
                )
                sealed class Artifact {
                    data class EvaluationOutput(val steps: List<Step>) : Artifact()
                }
            }

            data class InternalError(val throwable: Throwable) : Result()

            data class UserError(val detail: Any?) : Result()
        }
    }

    data class LogMessageReceived(val phaseId: UUID, val stream: String, val message: String) : BuildEvent()

    companion object {
        // To make testing easier given one can't have zero-arg data classes
        val BUILD_CANCELLED = BuildCancelled()
        val BUILD_SUCCEEDED = BuildSucceeded()
        val BUILD_FAILED = BuildFailed()
    }
}
