package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.logging.logger
import io.quartic.common.secrets.SecretsCodec
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.db.DatabaseBuilder
import io.quartic.eval.api.model.CytoscapeDag
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.apis.Database
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
            register(EvalResource(evaluator(configuration, environment).channel))
            register(QueryResource(defaultPipeline()))      // TODO: remove default pipeline
        }
    }

    private fun defaultPipeline() =
        OBJECT_MAPPER.readValue<CytoscapeDag>(javaClass.getResourceAsStream("/pipeline.json"))

    private fun evaluator(configuration: EvalConfiguration, environment: Environment): ActorJob<TriggerDetails> {
        val evaluator = Evaluator(
            clientBuilder.retrofit(configuration.registryUrl),
            qube(configuration),
            github(configuration),
            database(configuration, environment),
            clientBuilder
        )
        return actor(CommonPool, UNLIMITED) {
            for (details in channel) evaluator.evaluateAsync(details)
        }
    }

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

    private fun database(config: EvalConfiguration, environment: Environment) =
        DatabaseBuilder(
            EvalApplication::class.java,
            config.database,
            environment,
            SecretsCodec(config.masterKeyBase64)
        ).dao<Database>()


    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)
    }
}
