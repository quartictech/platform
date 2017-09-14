package io.quartic.home.graphql

import graphql.annotations.*
import io.quartic.eval.api.model.ApiBuildEvent
import java.time.Instant

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
    BuildEvent.Log::class,
    BuildEvent.Other::class,
    BuildEvent.PhaseStarted::class,
    BuildEvent.PhaseCompleted::class
))
interface BuildEvent {
    @GraphQLField
    fun time(): Long

    @GraphQLField
    fun type(): String

    @From<ApiBuildEvent.Log>
    data class Log (
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
    }

    @From<ApiBuildEvent.PhaseStarted>
    data class PhaseStarted(
        @get:GraphQLField
        @get:GraphQLName("phase_id")
        val phaseId: String,

        @GraphQLField
        val description: String,

        private val time: Instant
    ): BuildEvent {
        override fun time() = time.epochSecond
        override fun type() = "phase_started"
    }

    @From<ApiBuildEvent.PhaseCompleted>
    data class PhaseCompleted(
        @get:GraphQLField
        @get:GraphQLName("phase_id")
        val phaseId: String,

        private val time: Instant
    ): BuildEvent {
        override fun time() = time.epochSecond
        override fun type() = "phase_completed"
    }

    data class Other(private val time: Long): BuildEvent {
        override fun time(): Long = time

        override fun type(): String = "other"
    }

}
