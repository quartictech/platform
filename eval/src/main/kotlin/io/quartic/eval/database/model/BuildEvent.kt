package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.common.model.CustomerId
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger.GithubWebhook
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger.Manual
import io.quartic.eval.database.model.CurrentTriggerReceived.BuildTrigger.Automated
import io.quartic.eval.database.model.CurrentTriggerReceived.TriggerType.EVALUATE
import io.quartic.eval.database.model.CurrentTriggerReceived.TriggerType.EXECUTE
import io.quartic.eval.database.model.LegacyPhaseCompleted.*
import io.quartic.eval.database.model.PhaseCompletedV8.Node
import io.quartic.eval.database.model.PhaseCompletedV8.Result.*
import io.quartic.quarty.api.model.Pipeline
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
        Type(Manual::class, name = "manual"),
        Type(Automated::class, name = "automated")
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

        data class Automated(
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

data class PhaseCompletedV8(val phaseId: UUID, val result: Result): BuildEvent() {
    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Result.Success::class, name = "success"),
        Type(Result.InternalError::class, name = "internal_error"),
        Type(Result.UserError::class, name = "user_error")
    )
    sealed class Result {
        data class Success(val artifact: Artifact? = null) : Result()
        class InternalError : Result()
        data class UserError(val info: UserErrorInfo): Result()
    }

    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(UserErrorInfo.InvalidDag::class, name = "invalid_dag"),
        Type(UserErrorInfo.OtherException::class, name = "other_exception")
    )
    sealed class UserErrorInfo {
        data class InvalidDag(val error: String, val nodes: List<Node>) : UserErrorInfo()
        data class OtherException(val detail: Any?): UserErrorInfo()
    }

    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Artifact.EvaluationOutput::class, name = "evaluation_output"),
        Type(Artifact.NodeExecution::class, name = "node_execution")
    )
    sealed class Artifact {
        data class EvaluationOutput(val nodes: List<Node>) : Artifact()
        data class NodeExecution(val skipped: Boolean) : Artifact()
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        Type(Node.Step::class, name = "step"),
        Type(Node.Raw::class, name = "raw")
    )
    sealed class Node {
        abstract val id: String
        abstract val name: String
        abstract val metadata: Map<String, Any>
        abstract val info: V2.LexicalInfo
        abstract val inputs: List<V1.Dataset>
        abstract val output: V1.Dataset

        data class Step(
            override val id: String,
            override val name: String,
            override val metadata: Map<String, Any>,
            override val info: V2.LexicalInfo,
            override val inputs: List<V1.Dataset>,
            override val output: V1.Dataset
        ) : Node()

        data class Raw(
            override val id: String,
            override val name: String,
            override val metadata: Map<String, Any>,
            override val info: V2.LexicalInfo,
            val source: V2.Source,
            override val output: V1.Dataset
        ) : Node() {
            @JsonIgnore
            override val inputs = emptyList<V1.Dataset>()
        }
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
typealias PhaseCompleted = PhaseCompletedV8
typealias LogMessageReceived = CurrentLogMessageReceived


/**
 * Helper stuff
 */

// To make testing easier given one can't have zero-arg data classes
val BUILD_CANCELLED = BuildCancelled()
val BUILD_SUCCEEDED = BuildSucceeded()
val INTERNAL_ERROR = InternalError()

fun Pipeline.Node.toDatabaseModel() = when (this) {
    is Pipeline.Node.Step -> Node.Step(
        id = id,
        name = name,
        metadata = metadata,
        info = info.toDatabaseModel(),
        inputs = inputs.map { it.toDatabaseModel() },
        output = output.toDatabaseModel()
    )
    is Pipeline.Node.Raw -> Node.Raw(
        id = id,
        name = name,
        metadata = metadata,
        info = info.toDatabaseModel(),
        source = source.toDatabaseModel(),
        output = output.toDatabaseModel()
    )
}

fun Pipeline.LexicalInfo.toDatabaseModel() = V2.LexicalInfo(
    name = name,
    description = description,
    file = file,
    lineRange = lineRange
)

fun Pipeline.Dataset.toDatabaseModel() = V1.Dataset(
    namespace = this.namespace,
    datasetId = this.datasetId
)

fun Pipeline.Source.toDatabaseModel() = when (this) {
    is Pipeline.Source.Bucket -> V2.Source.Bucket(
        name = name,
        key = key
    )
}

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
    is Automated -> io.quartic.eval.api.model.BuildTrigger.Automated(
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
    is io.quartic.eval.api.model.BuildTrigger.Automated -> Automated(
        user = user,
        timestamp = timestamp,
        customerId = customerId,
        branch = branch,
        triggerType = when (triggerType) {
            io.quartic.eval.api.model.BuildTrigger.TriggerType.EVALUATE -> EVALUATE
            io.quartic.eval.api.model.BuildTrigger.TriggerType.EXECUTE -> EXECUTE
        }
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
