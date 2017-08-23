package io.quartic.qube.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.quartic.qube.api.model.ContainerSpec

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QubeRequest.Create::class, name = "create"),
    JsonSubTypes.Type(value = QubeRequest.Destroy::class, name = "destroy")
)
sealed class QubeRequest {
    abstract val name: String

    data class Create(
        override val name: String,
        val container: ContainerSpec
    ): QubeRequest()
    data class Destroy(override val name: String): QubeRequest()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QubeResponse.Waiting::class, name = "waiting"),
    JsonSubTypes.Type(value = QubeResponse.Running::class, name = "running"),
    JsonSubTypes.Type(value = QubeResponse.Terminated.Failed::class, name = "failed"),
    JsonSubTypes.Type(value = QubeResponse.Terminated.Succeeded::class, name = "succeeded"),
    JsonSubTypes.Type(value = QubeResponse.Terminated.Exception::class, name = "exception")
)
sealed class QubeResponse {
    abstract val name: String

    sealed class Terminated(override val name: String, val message: String): QubeResponse() {
        data class Failed(override val name: String, val errorMessage: String) : Terminated(name, errorMessage)
        data class Succeeded(override val name: String) : Terminated(name, "Finished with success")
        data class Exception(override val name: String) : Terminated(name, "Finished with failure")
    }

    data class Waiting(override val name: String): QubeResponse()
    data class Running(override val name: String, val hostname: String): QubeResponse()


}
