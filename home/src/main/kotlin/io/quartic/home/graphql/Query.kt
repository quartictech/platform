package io.quartic.home.graphql

import graphql.annotations.GraphQLDataFetcher
import graphql.annotations.GraphQLDescription
import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName

interface Query {
    @GraphQLField
    @GraphQLDescription("Feed of events for the authenticated user")
    @GraphQLDataFetcher(BuildsFetcher::class)
    fun feed(): List<Build>

    @GraphQLField
    @GraphQLDescription("Load a single build")
    @GraphQLDataFetcher(BuildFetcher::class)
    fun build(@GraphQLName("number") number: Long): Build

    @GraphQLField
    @GraphQLDescription("Fetch the currently authenticated user")
    @GraphQLDataFetcher(UserFetcher::class)
    fun profile(): User
}
