package io.quartic.qube.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.quartic.qube.api.model.PodSpec
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = QubeRequest.Create::class, name = "create"),
    JsonSubTypes.Type(value = QubeRequest.Destroy::class, name = "destroy")
)
sealed class QubeRequest {
    abstract val name: String

    data class Create(
        override val name: String,
        val pod: PodSpec
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

    sealed class Terminated(override val name: String): QubeResponse() {
        abstract val message: String?

        data class Failed(override val name: String, override val message: String?) : Terminated(name)
        data class Succeeded(override val name: String, override val message:String?) : Terminated(name)
        data class Exception(override val name: String, override val message: String?) : Terminated(name)
    }

    data class Waiting(override val name: String): QubeResponse()
    data class Running(override val name: String, val hostname: String, val containerId: UUID): QubeResponse()


}
