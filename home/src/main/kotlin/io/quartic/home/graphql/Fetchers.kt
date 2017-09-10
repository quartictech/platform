package io.quartic.home.graphql

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.quartic.common.logging.logger
import io.quartic.home.resource.GraphqlResource
import java.time.Instant

class BuildsFetcher: DataFetcher<List<Build>> {
    val LOG by logger()

    override fun get(env: DataFetchingEnvironment?): List<Build> {
        val context = env!!.getContext<GraphqlResource.Context>()
        val buildNumber: Long? = env.getArgument<Long>("buildNumber")
        LOG.info("fetching: ${buildNumber}")
        val x = context.eval.getBuildsAsync(context.user.customerId!!, buildNumber).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
        println(x)
        return x
    }
}

class BuildFetcher: DataFetcher<Build> {
    override fun get(env: DataFetchingEnvironment?): Build {
        val context = env!!.getContext<GraphqlResource.Context>()
        val buildNumber: Long? = env.getArgument<Long>("buildNumber")
        return context.eval.getBuildsAsync(context.user.customerId!!, buildNumber).get()
            .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond, emptyList()) }
            .first()
    }
}

class EventsFetcher: DataFetcher<List<IBuildEvent>> {
    override fun get(env: DataFetchingEnvironment?): List<IBuildEvent> {
        println("fetching events")
        val build = env!!.getSource<Build>()
        val context = env.getContext<GraphqlResource.Context>()
        return context.eval.getBuildEventsAsync(context.user.customerId!!, build.number).get()
            .map { event -> BuildEvent.Default(event.time.epochSecond) }
    }

}

