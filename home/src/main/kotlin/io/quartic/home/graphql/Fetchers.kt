package io.quartic.home.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.BuildTrigger

abstract class Fetcher<T> : DataFetcher<T> {
    override fun get(environment: DataFetchingEnvironment): T =
        get(environment.getContext(), environment)

    abstract fun get(context: GraphQLContext, env: DataFetchingEnvironment): T
}

class BuildsFetcher: Fetcher<List<Build>>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): List<Build> =
        context.eval.getBuildsAsync(context.user.customerId!!).get()
            .map { it.toGraphQL() }
}

class BuildFetcher: Fetcher<Build>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment) =
        context.eval.getBuildAsync(context.user.customerId!!, env.getArgument<Long>("number"))
            .get()
            .let { it.toGraphQL() }
}

fun io.quartic.eval.api.model.Build.toGraphQL(): Build =
    Build(
        this.id.toString(),
        this.buildNumber,
        this.branch,
        this.status,
        this.time.epochSecond,
        this.trigger.toGraphQL(),
        emptyList())

fun BuildTrigger.toGraphQL() = when (this) {
    is BuildTrigger.Manual -> Trigger("manual")
    is BuildTrigger.GithubWebhook -> Trigger("github_webhook")
}

class EventsFetcher: Fetcher<List<BuildEvent>>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): List<BuildEvent> {
        val build = env.getSource<Build>()
        return context.eval.getBuildEventsAsync(context.user.customerId!!, build.number).get()
            .map { it.toGraphQL() }
    }
}

fun ApiBuildEvent.toGraphQL() = when (this) {
    is ApiBuildEvent.Log -> BuildEvent.Log(
        this.phaseId.toString(),
        this.stream,
        this.message,
        this.time
    )
    is ApiBuildEvent.PhaseStarted -> BuildEvent.PhaseStarted(
        this.phaseId.toString(),
        this.description,
        this.time
    )
    is ApiBuildEvent.PhaseCompleted -> BuildEvent.PhaseCompleted(
        this.phaseId.toString(),
        this.time
    )
    else ->
        BuildEvent.Other(this.time.epochSecond)
}

class UserFetcher: Fetcher<User>() {
    override fun get(context: GraphQLContext, env: DataFetchingEnvironment): User {
        val githubUser = context.github.user((context.user.id.toInt()))
        return User(githubUser.name, githubUser.avatarUrl.toString())
    }
}
