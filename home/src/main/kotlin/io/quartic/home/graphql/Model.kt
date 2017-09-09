package io.quartic.home.graphql

import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName

data class Build(
    @GraphQLField
    @GraphQLName("id")
    val id: String,

    @GraphQLField
    @GraphQLName("number")
    val number: Long,

    @GraphQLField
    @GraphQLName("status")
    val status: String,

    @GraphQLField
    @GraphQLName("time")
    val time: Long,

    @GraphQLField
    @GraphQLName("type")
    val type: String = "build"
)
