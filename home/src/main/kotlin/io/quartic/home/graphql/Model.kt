package io.quartic.home.graphql

import graphql.annotations.GraphQLDataFetcher
import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLUnion

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

@GraphQLUnion(possibleTypes = arrayOf(BuildEvent.Default::class))
interface BuildEvent {
    @GraphQLField
    fun time(): Long

    @GraphQLField
    fun type(): String

    data class Default(val time: Long): BuildEvent {
        override fun type() = "default"
        override fun time() = time
    }
}
