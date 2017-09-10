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
    val status: String,

    @GraphQLField
    val time: Long,

    @GraphQLField
    @GraphQLDataFetcher(EventsFetcher::class)
    val events: List<IBuildEvent>,

    @GraphQLField
    val type: String = "build"

)

@GraphQLUnion(possibleTypes = arrayOf(BuildEvent.Default::class, BuildEvent.Noob2::class))
interface IBuildEvent {
    @GraphQLField
    fun time(): Long
}

sealed class BuildEvent : IBuildEvent {
    data class Default(val time: Long): BuildEvent() {
        override fun time() = time
    }

    data class Noob2(val time: Long): BuildEvent() {
        override fun time() = time
    }
}
