package io.quartic.home.graphql

import graphql.annotations.GraphQLDataFetcher
import graphql.annotations.GraphQLField

interface Query {
    @GraphQLField
    @GraphQLDataFetcher(BuildsFetcher::class)
    fun feed(): List<Build>

    @GraphQLField
    @GraphQLDataFetcher(BuildsFetcher::class)
    fun build(number: Long): Build
}
