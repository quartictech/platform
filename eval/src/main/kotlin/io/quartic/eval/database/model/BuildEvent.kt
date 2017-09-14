package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.Dataset
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.*
import io.quartic.eval.database.model.CurrentPhaseCompleted.Step
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
        data class EvaluationOutput(val steps: List<Step>) : Artifact()
    }

    data class Step(
        val id: String,
        val name: String,
        val description: String?,
        val file: String,
        val lineRange: List<Int>,
        val inputs: List<Dataset>,
        val outputs: List<Dataset>
    )

    data class Dataset(
        val namespace: String?,
        val datasetId: String
    ) {
        @get:JsonIgnore
        val fullyQualifiedName get() = "${namespace ?: ""}::${datasetId}"
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


/**
 * Helper stuff
 */

// To make testing easier given one can't have zero-arg data classes
val BUILD_CANCELLED = BuildCancelled()
val BUILD_SUCCEEDED = BuildSucceeded()

fun io.quartic.quarty.api.model.Step.toDatabaseModel() = Step(
    id = this.id,
    name = this.name,
    description = this.description,
    file = this.file,
    lineRange = this.lineRange,
    inputs = this.inputs.map { it.toDatabaseModel() },
    outputs = this.outputs.map { it.toDatabaseModel() }
)

fun io.quartic.quarty.api.model.Dataset.toDatabaseModel() = Dataset(
    namespace = this.namespace,
    datasetId = this.datasetId
)

