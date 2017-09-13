package io.quartic.home.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

abstract class Fetcher<T> : DataFetcher<T> {
    override fun get(environment: DataFetchingEnvironment): T =
        get(environment.getContext(), environment)

    abstract fun get(context: GraphQLContext, env: DataFetchingEnvironment): T
}

class BuildsFetcher: Fetcher<List<Build>>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): List<Build> =
        context.eval.getBuildsAsync(context.user.customerId!!).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
}

class BuildFetcher: Fetcher<Build>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment) =
        context.eval.getBuildAsync(context.user.customerId!!, env.getArgument<Long>("number"))
            .get()
            .let { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
}

class EventsFetcher: Fetcher<List<BuildEvent>>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): List<BuildEvent> {
        val build = env.getSource<Build>()
        return context.eval.getBuildEventsAsync(context.user.customerId!!, build.number).get()
            .map { event -> BuildEvent.Default(event.time.epochSecond) }
    }
}

class UserFetcher: Fetcher<User>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): User {
        val githubUser = context.github.user((context.user.id.toInt()))
        return User(githubUser.name, githubUser.avatarUrl.toString())
    }
}
