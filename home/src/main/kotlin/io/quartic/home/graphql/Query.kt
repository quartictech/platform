package io.quartic.home.graphql

import graphql.annotations.GraphQLDataFetcher
import graphql.annotations.GraphQLField
import graphql.annotations.GraphQLName
import io.quartic.eval.api.model.Build
import io.quartic.home.resource.GraphqlResource

interface Query {
    @GraphQLField
    @GraphQLName("feed")
    @GraphQLDataFetcher(GraphqlResource.BuildsFetcher::class)
    fun feed(): List<Build>
}
