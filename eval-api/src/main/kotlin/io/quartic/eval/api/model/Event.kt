package io.quartic.eval.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import java.time.Instant
import java.util.*

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ApiBuildEvent.Log::class, name = "log"),
    JsonSubTypes.Type(ApiBuildEvent.PhaseStarted::class, name = "phase_started"),
    JsonSubTypes.Type(ApiBuildEvent.PhaseCompleted::class, name = "phase_completed"),
    JsonSubTypes.Type(ApiBuildEvent.Other::class, name = "other")
)
sealed class ApiBuildEvent {
    abstract val id: UUID
    abstract val time: Instant

    data class Log(
        @JsonProperty("phase_id")
        val phaseId: UUID,
        val stream: String,
        val message: String,
        override val id: UUID,
        override val time: Instant
    ): ApiBuildEvent()

    data class PhaseStarted(
        @JsonProperty("phase_id")
        val phaseId: UUID,
        val description: String,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class PhaseCompleted(
        @JsonProperty("phase_id")
        val phaseId: UUID,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class Other(override val time: Instant, override val id: UUID): ApiBuildEvent()
}
