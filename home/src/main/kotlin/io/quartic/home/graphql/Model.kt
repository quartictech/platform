package io.quartic.home.graphql

import graphql.annotations.*
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.ApiPhaseCompletedResult
import java.time.Instant
import java.util.*

data class Build(
    @GraphQLField
    val id: String,

    @GraphQLField
    val number: Long,

    @GraphQLField
    val branch: String,

    @GraphQLField
    val status: String,

    @GraphQLField
    val time: Long,

    @GraphQLField
    val trigger: Trigger,

    @GraphQLField
    @GraphQLDataFetcher(EventsFetcher::class)
    val events: List<BuildEvent>,

    @GraphQLField
    val type: String = "build"
)

data class Trigger(
    @GraphQLField
    val type: String
)

data class User(
    @GraphQLField
    val name: String,

    @GraphQLField
    val avatarUrl: String
)

@GraphQLUnion(possibleTypes = arrayOf(
    PhaseCompletedResult.Success::class,
    PhaseCompletedResult.UserError::class,
    PhaseCompletedResult.InternalError::class
))
interface PhaseCompletedResult {
    @From<ApiPhaseCompletedResult.Success>
    class Success: PhaseCompletedResult

    @From<ApiPhaseCompletedResult.UserError>
    data class UserError(
        @GraphQLField
        val error: String
    ): PhaseCompletedResult

    @From<ApiPhaseCompletedResult.InternalError>
    class InternalError: PhaseCompletedResult
}

@GraphQLUnion(possibleTypes = arrayOf(
    BuildEvent.Log::class,
    BuildEvent.Other::class,
    BuildEvent.PhaseStarted::class,
    BuildEvent.PhaseCompleted::class,
    BuildEvent.TriggerReceived::class,
    BuildEvent.BuildFailed::class
))
interface BuildEvent {
    @GraphQLField
    fun id(): String

    @GraphQLField
    fun time(): Long

    @GraphQLField
    fun type(): String

    @From<ApiBuildEvent.Log>
    data class Log (
        private val id: UUID,

        @get:GraphQLField
        @get:GraphQLName("phase_id")
        val phaseId: String,

        @GraphQLField
        val stream: String,

        @GraphQLField
        val message: String,

        private val time: Instant
    ): BuildEvent {
        override fun type() = "log"
        override fun time() = time.epochSecond
        override fun id() = id.toString()
    }

    @From<ApiBuildEvent.TriggerReceived>
    data class TriggerReceived (
        @get:GraphQLField
        @get:GraphQLName("trigger_type")
        val triggerType: String,

        private val id: UUID,

        private val time: Instant
    ): BuildEvent {
        override fun type() = "trigger_received"
        override fun time() = time.epochSecond
        override fun id() = id.toString()
    }

    @From<ApiBuildEvent.PhaseStarted>
    data class PhaseStarted(
        private val id: UUID,

        @get:GraphQLField
        @get:GraphQLName("phase_id")
        val phaseId: String,

        @GraphQLField
        val description: String,

        private val time: Instant
    ): BuildEvent {
        override fun time() = time.epochSecond
        override fun type() = "phase_started"
        override fun id() = id.toString()
    }

    @From<ApiBuildEvent.PhaseCompleted>
    data class PhaseCompleted(
        private val id: UUID,

        @GraphQLField
        val result: PhaseCompletedResult,

        @get:GraphQLField
        @get:GraphQLName("phase_id")
        val phaseId: String,

        private val time: Instant
    ): BuildEvent {
        override fun time() = time.epochSecond
        override fun type() = "phase_completed"
        override fun id() = id.toString()
    }

    @From<ApiBuildEvent.BuildFailed>
    data class BuildFailed(
        @GraphQLField
        val description: String,
        private val id: UUID,
        private val time: Instant
    ): BuildEvent {
        override fun time() = time.epochSecond
        override fun type() = "build_failed"
        override fun id() = id.toString()
    }

    data class Other(private val id: UUID, private val time: Long): BuildEvent {
        override fun time(): Long = time
        override fun type(): String = "other"
        override fun id() = id.toString()
    }

}
