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
    JsonSubTypes.Type(value = Request.CreatePod::class, name = "create"),
    JsonSubTypes.Type(value = Request.DestroyPod::class, name = "destroy")
)
sealed class Request {
    data class CreatePod(
        val name: String,
        val image: String,
        val command: List<String>
    ): Request()
    data class DestroyPod(val name: String): Request()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Response.PodWaiting::class, name = "waiting"),
    JsonSubTypes.Type(value = Response.PodRunning::class, name = "running"),
    JsonSubTypes.Type(value = Response.PodFailed::class, name = "failed"),
    JsonSubTypes.Type(value = Response.PodSucceeded::class, name = "succeeded")
)
sealed class Response {
    data class PodWaiting(val name: String): Response()
    data class PodRunning(val name: String, val hostname: String): Response()
    data class PodFailed(val name: String): Response()
    data class PodSucceeded(val name: String): Response()
    data class PodException(val name: String): Response()
}
