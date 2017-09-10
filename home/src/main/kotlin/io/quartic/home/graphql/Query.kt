package io.quartic.home.graphql

import graphql.annotations.GraphQLDataFetcher
import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName

interface Query {
    @GraphQLField
    @GraphQLDataFetcher(BuildsFetcher::class)
    fun feed(): List<Build>

    @GraphQLField
    @GraphQLDataFetcher(BuildFetcher::class)
    fun build(@GraphQLName("buildNumber") number: Long): Build
}
