package io.quartic.quarty.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.quartic.quarty.model.QuartyMessage.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(Progress::class, name = "progress"),
    JsonSubTypes.Type(Log::class, name = "log"),
    JsonSubTypes.Type(Result::class, name = "result"),
    JsonSubTypes.Type(Error::class, name = "error")
)
sealed class QuartyMessage {
    data class Progress(val message: String) : QuartyMessage()
    data class Log(val stream: String, val line: String) : QuartyMessage()
    data class Result(val result: List<Step>) : QuartyMessage()
    data class Error(val detail: Any) : QuartyMessage()
}
