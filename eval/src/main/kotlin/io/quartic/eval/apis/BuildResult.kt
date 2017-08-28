package io.quartic.eval.apis

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.quartic.quarty.model.Step

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
sealed class BuildResult {
    val version: Long = 1

    @JsonTypeName("success")
    data class Success(val dag: List<Step>) : BuildResult()

    @JsonTypeName("internal_error")
    data class InternalError(val throwable: Throwable) : BuildResult()

    @JsonTypeName("user_error")
    data class UserError(val detail: Any?) : BuildResult()
}
