package io.quartic.eval.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.Companion.VERSION
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.quarty.model.Step
import java.util.*

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(TriggerReceived::class, name = "trigger_received_${VERSION}"),
    Type(BuildCancelled::class, name = "build_cancelled_${VERSION}"),
    Type(BuildSucceeded::class, name = "build_succeeded_${VERSION}"),
    Type(BuildFailed::class, name = "build_failed_${VERSION}"),
    Type(ContainerAcquired::class, name = "container_acquired_${VERSION}"),
    Type(PhaseStarted::class, name = "phase_started_${VERSION}"),
    Type(PhaseCompleted::class, name = "phase_completed_${VERSION}"),
    Type(LogMessageReceived::class, name = "log_message_received_${VERSION}")
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

        const val VERSION = "v1"
    }
}
