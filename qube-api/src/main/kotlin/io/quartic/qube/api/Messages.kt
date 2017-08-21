package io.quartic.qube.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class Status {
    UNKNOWN,
    PENDING,
    RUNNING,
    ERROR,
    SUCCESS
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ReceivedMessage.CreatePod::class, name = "create"),
    JsonSubTypes.Type(value = ReceivedMessage.RemovePod::class, name = "remove")
)
sealed class ReceivedMessage {
    data class CreatePod(
        val name: String,
        val image: String,
        val command: List<String>
    ): ReceivedMessage()
    data class RemovePod(val name: String): ReceivedMessage()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SentMessage.PodStatus::class, name = "status"),
    JsonSubTypes.Type(value = SentMessage.PodDeleted::class, name = "deleted")
)
sealed class SentMessage {
    data class PodStatus(val name: String, val status: Status, val hostname: String?): SentMessage()
    data class PodDeleted(val name: String) : SentMessage()
}
