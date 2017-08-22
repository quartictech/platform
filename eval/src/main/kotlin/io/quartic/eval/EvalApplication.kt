package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import io.quartic.qube.api.model.TriggerDetails
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.actor

class EvalApplication : ApplicationBase<EvalConfiguration>() {
    override fun runApplication(configuration: EvalConfiguration, environment: Environment) {
        val evaluator = evaluator(configuration)
        environment.jersey().register(EvalResource(evaluator.channel))
    }

    private fun evaluator(configuration: EvalConfiguration): ActorJob<TriggerDetails> {
        val evaluator = Evaluator(
            clientBuilder.retrofit(configuration.registryUrl),
            qube(configuration),
            github(configuration),
            database,
            clientBuilder
        )
        return actor(CommonPool, UNLIMITED) {
            for (details in channel) evaluator.evaluateAsync(details)
        }
    }

    // TODO - do this properly
    private val database = object : Database {
        override fun writeResult(result: BuildResult) {
            throw UnsupportedOperationException("not implemented")
        }
    }

    private fun github(config: EvalConfiguration) = GitHubInstallationClient(
        config.github.appId,
        config.github.apiRootUrl,
        config.secretsCodec.decrypt(config.github.privateKeyEncrypted),
        clientBuilder
    )

    private fun qube(config: EvalConfiguration) = QubeProxy.create(WebsocketClientImpl.create(config.qubeUrl))

    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)
    }
}
