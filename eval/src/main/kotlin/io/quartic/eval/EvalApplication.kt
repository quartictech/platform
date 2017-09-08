package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.db.DatabaseBuilder
import io.quartic.common.secrets.SecretsCodec
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.sequencer.SequencerImpl
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.actor

class EvalApplication : ApplicationBase<EvalConfiguration>() {
    override fun runApplication(configuration: EvalConfiguration, environment: Environment) {
        val database = database(configuration, environment)

        with(environment.jersey()) {
            register(EvalResource(evaluator(configuration, database).channel))
            register(QueryResource(database))
        }
    }

    private fun evaluator(config: EvalConfiguration, database: Database): ActorJob<BuildTrigger> {
        val evaluator = Evaluator(
            sequencer(config, database),
            clientBuilder.retrofit(config.registryUrl),
            github(config),
            clientBuilder
        )
        return actor(CommonPool, UNLIMITED) {
            for (trigger in channel) evaluator.evaluateAsync(trigger)
        }
    }

    private fun sequencer(config: EvalConfiguration, database: Database) = SequencerImpl(
        qube(config),
        database,
        notifier(config)
    )

    private fun notifier(config: EvalConfiguration) = Notifier(
        clientBuilder.retrofit(config.heyUrl),
        github(config),
        config.homeUrlFormat
    )

    private fun github(config: EvalConfiguration) = GitHubInstallationClient(
        config.github.appId,
        config.github.apiRootUrl,
        config.github.privateKeyEncrypted.decrypt(),
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
