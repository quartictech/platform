package io.quartic.eval.api.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ApiPhaseCompletedResult.Success::class, name = "success"),
    JsonSubTypes.Type(ApiPhaseCompletedResult.UserError::class, name = "user_error"),
    JsonSubTypes.Type(ApiPhaseCompletedResult.InternalError::class, name = "internal_error")
)
sealed class ApiPhaseCompletedResult {
    class Success: ApiPhaseCompletedResult()
    class UserError(val error: String): ApiPhaseCompletedResult()
    class InternalError: ApiPhaseCompletedResult()
}
