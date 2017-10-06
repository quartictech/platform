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
    JsonSubTypes.Type(ApiBuildEvent.PhaseStarted::class, name = "phase_started"),
    JsonSubTypes.Type(ApiBuildEvent.PhaseCompleted::class, name = "phase_completed"),
    JsonSubTypes.Type(ApiBuildEvent.TriggerReceived::class, name = "trigger_received"),
    JsonSubTypes.Type(ApiBuildEvent.BuildFailed::class, name = "build_failed"),
    JsonSubTypes.Type(ApiBuildEvent.BuildSucceeded::class, name = "build_succeeded"),
    JsonSubTypes.Type(ApiBuildEvent.Other::class, name = "other")
)
sealed class ApiBuildEvent {
    abstract val id: UUID
    abstract val time: Instant

    data class Log(
        val phaseId: UUID,
        val stream: String,
        val message: String,
        override val id: UUID,
        override val time: Instant
    ): ApiBuildEvent()

    data class PhaseStarted(
        val phaseId: UUID,
        val description: String,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class PhaseCompleted(
        val phaseId: UUID,
        val result: ApiPhaseCompletedResult,
        val skipped: Boolean,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class TriggerReceived(
        val triggerType: String,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class BuildSucceeded(
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class BuildFailed(
        val description: String,
        override val time: Instant,
        override val id: UUID
    ): ApiBuildEvent()

    data class Other(override val time: Instant, override val id: UUID): ApiBuildEvent()
}
