package io.quartic.home.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.quartic.home.resource.GraphqlResource

class BuildsFetcher: DataFetcher<List<Build>> {
    override fun get(env: DataFetchingEnvironment?): List<Build> {
        val context = env!!.getContext<GraphqlResource.Context>()
        return context.eval.getBuildsAsync(context.user.customerId!!).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond) }
    }
}

