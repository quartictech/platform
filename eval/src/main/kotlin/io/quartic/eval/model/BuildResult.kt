package io.quartic.eval.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.quartic.eval.model.BuildResult.*
import io.quartic.quarty.model.Step

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes(
    JsonSubTypes.Type(Success::class, name = "success"),
    JsonSubTypes.Type(InternalError::class, name = "internal_error"),
    JsonSubTypes.Type(UserError::class, name = "user_error")
)
sealed class BuildResult {
    var version: Int = 1

    fun checkVersion() {
        if(version != VERSION) {
            throw IllegalStateException("Version mismatch: ${version} != ${VERSION}")
        }
    }

    data class Success(val steps: List<Step>) : BuildResult()

    data class InternalError(val throwable: Throwable) : BuildResult()

    data class UserError(val detail: Any?) : BuildResult()

    companion object {
        val VERSION = 1
    }
}
