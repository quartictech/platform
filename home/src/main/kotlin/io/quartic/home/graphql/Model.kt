package io.quartic.home.graphql

import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName

data class Build(
    @GraphQLField
    @GraphQLName("id")
    val id: String,

    @GraphQLField
    val number: Long,

    @GraphQLField
    val status: String,

    @GraphQLField
    val time: Long,

    @GraphQLField
    val type: String = "build"
)
