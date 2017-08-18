package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.eval.apis.GitHubClient
import io.quartic.eval.apis.QubeProxy
import io.quartic.github.GithubInstallationClient.GitHubInstallationAccessToken
import io.quartic.qube.api.model.TriggerDetails
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import java.util.concurrent.CompletableFuture

class EvalApplication : ApplicationBase<EvalConfiguration>() {
    override fun runApplication(configuration: EvalConfiguration, environment: Environment) {
        val evaluator = evaluator(configuration)
        environment.jersey().register(EvalResource(evaluator.channel))
    }

    private fun evaluator(configuration: EvalConfiguration): ActorJob<TriggerDetails> {
        val evaluator = Evaluator(
            clientBuilder.retrofit(configuration.registryUrl),
            qube,
            github,
            database,
            clientBuilder
        )
        return actor(CommonPool, UNLIMITED) {
            for (details in channel) evaluator.evaluate(details)
        }
    }

    // TODO - do this properly
    private val database = object : Database {
        override fun writeResult(result: BuildResult) {
            throw UnsupportedOperationException("not implemented")
        }
    }

    // TODO - do this properly
    private val github = object : GitHubClient {
        override fun getAccessTokenAsync(installationId: Long): CompletableFuture<GitHubInstallationAccessToken> {
            throw UnsupportedOperationException("not implemented")
        }
    }

    // TODO - do this properly
    private val qube = object : QubeProxy {
        override fun enqueue(): ReceiveChannel<QubeProxy.QubeEvent> {
            throw UnsupportedOperationException("not implemented")
        }
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)
    }
}
