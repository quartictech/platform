package io.quartic.eval.api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import java.time.Instant
import java.util.*

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ApiBuildEvent.Log::class, name = "log"),
    JsonSubTypes.Type(ApiBuildEvent.Other::class, name = "other")
)
sealed class ApiBuildEvent {
    abstract val time: Instant

    data class Log(
        val phaseId: UUID,
        val stream: String,
        val message: String,
        override val time: Instant
    ) : ApiBuildEvent()

    data class Other(override val time: Instant): ApiBuildEvent()
}
