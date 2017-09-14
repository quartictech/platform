package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.common.model.CustomerId
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.Dataset
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.*
import io.quartic.eval.database.model.CurrentPhaseCompleted.Step
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger.GithubWebhook
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger.Manual
import io.quartic.eval.database.model.CurrentTriggerReceived.TriggerType.EVALUATE
import io.quartic.eval.database.model.CurrentTriggerReceived.TriggerType.EXECUTE
import java.time.Instant
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
data class CurrentTriggerReceived(val trigger: BuildTrigger) : BuildEvent() {
    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(GithubWebhook::class, name = "github_webhook"),
        Type(Manual::class, name = "manual")
    )
    sealed class BuildTrigger {
        data class GithubWebhook(
            val deliveryId: String,
            val repoId: Long,
            val ref: String,
            val commit: String,
            val timestamp: Instant,
            val repoName: String,
            val repoOwner: String,
            val installationId: Long,
            val rawWebhook: Map<String, Any>
        ): BuildTrigger()

        data class Manual(
            val user: String,
            val timestamp: Instant,
            val customerId: CustomerId,
            val branch: String,
            val triggerType: TriggerType
        ): BuildTrigger()
    }

    enum class TriggerType {
        EVALUATE,
        EXECUTE
    }
}
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

fun BuildTrigger.toApiModel() = when (this) {
    is GithubWebhook -> io.quartic.eval.api.model.BuildTrigger.GithubWebhook(
        deliveryId = deliveryId,
        repoId = repoId,
        ref = ref,
        commit = commit,
        timestamp = timestamp,
        repoName = repoName,
        repoOwner = repoOwner,
        installationId = installationId,
        rawWebhook = rawWebhook
    )
    is Manual -> io.quartic.eval.api.model.BuildTrigger.Manual(
        user = user,
        timestamp = timestamp,
        customerId = customerId,
        branch = branch,
        triggerType = when (triggerType) {
            EVALUATE -> io.quartic.eval.api.model.BuildTrigger.TriggerType.EVALUATE
            EXECUTE -> io.quartic.eval.api.model.BuildTrigger.TriggerType.EXECUTE
        }
    )
}

fun io.quartic.eval.api.model.BuildTrigger.toDatabaseModel() = when (this) {
    is io.quartic.eval.api.model.BuildTrigger.GithubWebhook -> GithubWebhook(
        deliveryId = deliveryId,
        repoId = repoId,
        ref = ref,
        commit = commit,
        timestamp = timestamp,
        repoName = repoName,
        repoOwner = repoOwner,
        installationId = installationId,
        rawWebhook = rawWebhook
    )
    is io.quartic.eval.api.model.BuildTrigger.Manual -> Manual(
        user = user,
        timestamp = timestamp,
        customerId = customerId,
        branch = branch,
        triggerType = when (triggerType) {
            io.quartic.eval.api.model.BuildTrigger.TriggerType.EVALUATE -> EVALUATE
            io.quartic.eval.api.model.BuildTrigger.TriggerType.EXECUTE -> EXECUTE
        }
    )
}
