package io.quartic.eval

import io.dropwizard.setup.Environment
import io.quartic.common.application.ApplicationBase
import io.quartic.common.db.DatabaseBuilder
import io.quartic.eval.database.Database
import io.quartic.eval.pruner.Pruner
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.sequencer.BuildInitiator
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import io.quartic.eval.sequencer.SequencerImpl
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.qube.api.model.PodSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.experimental.channels.actor
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.CompletableFuture

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
            pruner(config)
        )
        return actor(CommonPool, UNLIMITED) {
            for (build in channel) evaluator.evaluateAsync(build)
        }
    }

    private fun buildInitiator(config: EvalConfiguration, database: Database) = BuildInitiator(
        database,
        clientBuilder.retrofit(config.registryUrl)
    )

    private fun pruner(config: EvalConfiguration) = Pruner(
        clientBuilder.retrofit(config.catalogueUrl),
        stubHowl()
    )

    // This emulates a 404 response from Howl for any request, which means that nothing will get pruned
    // TODO - switch this out for real calls to Howl once the endpoint is implemented
    private fun stubHowl() = object : HowlClient {
        override fun getUnmanagedMetadataAsync(targetNamespace: String, key: String): CompletableFuture<StorageMetadata> {
            val future = CompletableFuture<StorageMetadata>()
            future.completeExceptionally(HttpException(Response.error<Any>(404, ResponseBody.create(null, "Not found"))))
            return future
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

    companion object {
        @JvmStatic fun main(args: Array<String>) = EvalApplication().run(*args)

        const val QUARTIC_PYTHON_VERSION = "0.5.0"
    }
}
