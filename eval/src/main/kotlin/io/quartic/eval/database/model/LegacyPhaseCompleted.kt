package io.quartic.eval.database.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Artifact.EvaluationOutput
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Result.*
import java.util.*

class LegacyPhaseCompleted private constructor() {

    data class V1(val phaseId: UUID, val result: Result) {
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


}
