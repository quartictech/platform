package io.quartic.quarty.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(QuartyMessage.Progress::class, name = "progress"),
    JsonSubTypes.Type(QuartyMessage.Log::class, name = "log"),
    JsonSubTypes.Type(QuartyMessage.Result::class, name = "result"),
    JsonSubTypes.Type(QuartyMessage.Error::class, name = "error")
)
sealed class QuartyMessage {
    @JsonSubTypes.Type(value = QuartyMessage.Progress::class, name = "progress")
    data class Progress(val message: String) : QuartyMessage()

    @JsonSubTypes.Type(value = QuartyMessage.Log::class, name = "log")
    data class Log(val stream: String, val line: String) : QuartyMessage()

    @JsonSubTypes.Type(value = QuartyMessage.Result::class, name = "result")
    data class Result(val result: List<Step>) : QuartyMessage()

    @JsonSubTypes.Type(value = QuartyMessage.Error::class, name = "error")
    data class Error(val detail: Any) : QuartyMessage()
}
