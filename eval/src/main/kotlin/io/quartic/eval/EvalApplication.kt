package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.auth.internal.InternalTokenGenerator
import io.quartic.common.db.DatabaseBuilder
import io.quartic.eval.database.Database
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.sequencer.BuildInitiator
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import io.quartic.eval.sequencer.SequencerImpl
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.actor
import java.time.Duration

class EvalApplication : ApplicationBase<EvalConfiguration>() {
    override fun runApplication(configuration: EvalConfiguration, environment: Environment) {
        val database = database(configuration, environment)

        with(environment.jersey()) {
            register(TriggerResource(
                buildInitiator(configuration, database),
                evaluator(configuration, database).channel)
            )
            register(QueryResource(database))
        }
    }

    private fun evaluator(config: EvalConfiguration, database: Database): ActorJob<BuildContext> {
        val evaluator = Evaluator(
            sequencer(config, database),
            github(config),
            populator(config),
            tokenGenerator(config)
        )
        return actor(CommonPool, UNLIMITED) {
            for (build in channel) evaluator.evaluateAsync(build)
        }
    }

    private fun buildInitiator(config: EvalConfiguration, database: Database) = BuildInitiator(
        database,
        clientBuilder.retrofit(config.registryUrl)
    )

    private fun populator(config: EvalConfiguration) = RawPopulator(
        clientBuilder.retrofit(config.catalogueUrl),
        clientBuilder.retrofit(config.howlUrl)
    )

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
        injectPodEnvironment(config.qube.pod)
    )

    private fun injectPodEnvironment(podSpec: PodSpec) =
       podSpec.copy(podSpec.containers.map { containerSpec ->
           containerSpec.copy(env = containerSpec.env
               .plus("QUARTIC_PYTHON_VERSION" to QUARTIC_PYTHON_VERSION)
           )
       })


    private fun database(config: EvalConfiguration, environment: Environment) =
        DatabaseBuilder(
            EvalApplication::class.java,
            config.database,
            environment,
            config.secretsCodec
        ).dao<Database>()

    private fun tokenGenerator(config: EvalConfiguration) = InternalTokenGenerator(
        config.auth.signingKeyEncryptedBase64.decrypt(),
        Duration.ofSeconds(config.auth.timeToLiveSeconds.toLong())
    )

    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)

        const val QUARTIC_PYTHON_VERSION = "feature/fix-raw-2"
    }
}
