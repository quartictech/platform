package io.quartic.eval.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.BuildCompleted.*
import io.quartic.eval.model.BuildEvent.Companion.VERSION
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.quarty.api.model.Pipeline.Node
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
    data class TriggerReceived(
        val trigger: BuildTrigger
    ) : BuildEvent()

    sealed class BuildCompleted : BuildEvent() {
        class BuildCancelled : BuildCompleted()
        class BuildSucceeded : BuildCompleted()
        data class BuildFailed(val description: String) : BuildCompleted()
    }

    data class ContainerAcquired(val containerId: UUID, val hostname: String) : BuildEvent()

    data class PhaseStarted(val phaseId: UUID, val description: String) : BuildEvent()

    data class PhaseCompleted(val phaseId: UUID, val result: Result) : BuildEvent() {

        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(Success::class, name = "success"),
            Type(InternalError::class, name = "internal_error"),
            Type(UserError::class, name = "user_error")
        )
        sealed class Result {
            data class Success(val artifact: Artifact? = null) : Result() {

                @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
                @JsonSubTypes(
                    Type(EvaluationOutput::class, name = "evaluation_output")
                )
                sealed class Artifact {
                    // TODO - have our own Node to decouple DB schema
                    data class EvaluationOutput(val nodes: List<Node>) : Artifact()
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

        const val VERSION = "v1"
    }
}
