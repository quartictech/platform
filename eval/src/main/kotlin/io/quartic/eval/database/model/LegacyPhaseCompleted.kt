package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import java.util.*

class LegacyPhaseCompleted private constructor() {

    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(Type(V2::class, name = "phase_completed"))
    data class V2(val phaseId: UUID, val result: Result) {
        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V2.Result.Success::class, name = "success"),
            Type(V2.Result.InternalError::class, name = "internal_error"),
            Type(V2.Result.UserError::class, name = "user_error")
        )
        sealed class Result {
            data class Success(val artifact: Artifact? = null) : Result()
            data class InternalError(val throwable: Throwable) : Result()
            data class UserError(val detail: Any?) : Result()
        }

        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V2.Artifact.EvaluationOutput::class, name = "evaluation_output")
        )
        sealed class Artifact {
            data class EvaluationOutput(val nodes: List<Node>) : Artifact()
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V2.Node.Step::class, name = "step"),
            Type(V2.Node.Raw::class, name = "raw")
        )
        sealed class Node {
            abstract val id: String
            abstract val info: LexicalInfo
            abstract val inputs: List<V1.Dataset>
            abstract val output: V1.Dataset

            data class Step(
                override val id: String,
                override val info: LexicalInfo,
                override val inputs: List<V1.Dataset>,
                override val output: V1.Dataset
            ) : Node()

            data class Raw(
                override val id: String,
                override val info: LexicalInfo,
                val source: Source,
                override val output: V1.Dataset
            ) : Node() {
                @JsonIgnore
                override val inputs = emptyList<V1.Dataset>()
            }
        }

        data class LexicalInfo(
            val name: String,
            val description: String?,
            val file: String,
            val lineRange: List<Int>
        )

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V2.Source.Bucket::class, name = "bucket")
        )
        sealed class Source {
            data class Bucket(
                val key: String,
                val name: String? = null   // null means default bucket
            ) : Source()
        }
    }


    @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
    @JsonSubTypes(Type(V1::class, name = "phase_completed"))
    data class V1(val phaseId: UUID, val result: Result) {
        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V1.Result.Success::class, name = "success"),
            Type(V1.Result.InternalError::class, name = "internal_error"),
            Type(V1.Result.UserError::class, name = "user_error")
        )
        sealed class Result {
            data class Success(val artifact: Artifact? = null) : Result()
            data class InternalError(val throwable: Throwable) : Result()
            data class UserError(val detail: Any?) : Result()
        }

        @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
        @JsonSubTypes(
            Type(V1.Artifact.EvaluationOutput::class, name = "evaluation_output")
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
}
