package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.database.NoobDatabase
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.actor

class EvalApplication : ApplicationBase<EvalConfiguration>() {
    override fun runApplication(configuration: EvalConfiguration, environment: Environment) {
        with(environment.jersey()) {
            register(EvalResource(evaluator(configuration).channel))
            register(QueryResource(database))
        }
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

    private val database = NoobDatabase()   // TODO - do this properly

    private fun github(config: EvalConfiguration) = GitHubInstallationClient(
        config.github.appId,
        config.github.apiRootUrl,
        config.secretsCodec.decrypt(config.github.privateKeyEncrypted),
        clientBuilder
    )

    private fun qube(config: EvalConfiguration) = QubeProxy.create(
        WebsocketClientImpl.create(config.qube.url),
        config.qube.container
    )

    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)
    }
}
