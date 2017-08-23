package io.quartic.qube.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QubeRequest.Create::class, name = "create"),
    JsonSubTypes.Type(value = QubeRequest.Destroy::class, name = "destroy")
)
sealed class QubeRequest {
    abstract val name: String

    data class Create(
        override val name: String,
        val image: String,
        val command: List<String>
    ): QubeRequest()
    data class Destroy(override val name: String): QubeRequest()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QubeResponse.Waiting::class, name = "waiting"),
    JsonSubTypes.Type(value = QubeResponse.Running::class, name = "running"),
    JsonSubTypes.Type(value = QubeResponse.Failed::class, name = "failed"),
    JsonSubTypes.Type(value = QubeResponse.Succeeded::class, name = "succeeded"),
    JsonSubTypes.Type(value = QubeResponse.Exception::class, name = "exception")
)
sealed class QubeResponse {
    abstract val name: String

    open class Terminated(override val name: String, val message: String): QubeResponse()

    data class Waiting(override val name: String): QubeResponse()
    data class Running(override val name: String, val hostname: String): QubeResponse()

    data class Failed(override val name: String, val errorMessage: String): Terminated(name, errorMessage)
    data class Succeeded(override val name: String): Terminated(name, "Finished with success")
    data class Exception(override val name: String): Terminated(name, "Finished with failure")
}
