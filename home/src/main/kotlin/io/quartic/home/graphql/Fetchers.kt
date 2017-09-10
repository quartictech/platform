package io.quartic.home.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.quartic.home.resource.GraphqlResource

abstract class Fetcher<T> : DataFetcher<T> {
    override fun get(environment: DataFetchingEnvironment): T =
        get(environment.getContext(), environment)

    abstract fun get(context: GraphqlResource.Context, env: DataFetchingEnvironment): T
}

class BuildsFetcher: Fetcher<List<Build>>() {
    override fun get(context: GraphqlResource.Context, env: DataFetchingEnvironment): List<Build> =
        context.eval.getBuildsAsync(context.user.customerId!!, null).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
}

class BuildFetcher: Fetcher<Build>() {
    override fun get(context: GraphqlResource.Context, env: DataFetchingEnvironment): Build =
        context.eval.getBuildsAsync(context.user.customerId!!, env.getArgument<Long>("buildNumber")).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
            .first()
}

class EventsFetcher: DataFetcher<List<BuildEvent>> {
    override fun get(env: DataFetchingEnvironment?): List<BuildEvent> {
        val build = env!!.getSource<Build>()
        val context = env.getContext<GraphqlResource.Context>()
        return context.eval.getBuildEventsAsync(context.user.customerId!!, build.number).get()
            .map { event -> BuildEvent.Default(event.time.epochSecond) }
    }

}

