package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.logging.logger
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.eval.apis.Database.BuildResult.*
import io.quartic.eval.apis.GitHubClient
import io.quartic.eval.apis.QuartyClient
import io.quartic.eval.apis.QuartyClient.QuartyResult
import io.quartic.eval.apis.QubeProxy
import io.quartic.eval.apis.QubeProxy.QubeEvent
import io.quartic.eval.apis.QubeProxy.QubeEvent.ErrorEvent
import io.quartic.eval.apis.QubeProxy.QubeEvent.ReadyEvent
import io.quartic.qube.api.model.TriggerDetails
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.selects.select
import org.apache.http.client.utils.URIBuilder
import retrofit2.HttpException
import java.util.concurrent.CompletableFuture

// TODO - retries
// TODO - timeouts
class Evaluator(
    private val registry: RegistryServiceClient,
    private val qube: QubeProxy,
    private val github: GitHubClient,
    private val database: Database,
    private val quartyBuilder: (String) -> QuartyClient
) {
    constructor(
        registry: RegistryServiceClient,
        qube: QubeProxy,
        github: GitHubClient,
        database: Database,
        clientBuilder: ClientBuilder
    ) : this(registry, qube, github, database, { hostname -> clientBuilder.retrofit("http://${hostname}") })

    private val LOG by logger()

    suspend fun evaluate(trigger: TriggerDetails) {
        if (buildShouldProceed(trigger)) {
            val result = runBuild(trigger)
            database.writeResult(result)
        }
    }

    private suspend fun buildShouldProceed(trigger: TriggerDetails) = try {
        registry.getCustomerAsync(null, trigger.repoId).await()
        true
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: Exception) {
        if (e is HttpException && e.code() == 404) {
            LOG.warn("Repo ID ${trigger.repoId} not found in Registry")
        } else {
            LOG.error("Error communicating with Registry", e)
        }
        false
    }

    private suspend fun runBuild(trigger: TriggerDetails): BuildResult {
        val channel = qube.enqueue()

        return try {
            val hostname = awaitHostname(channel)
            val getDag = getDagAsync(hostname, trigger)
            try {
                select {
                    getDag.onAwait { transformQuartyResult(it) }
                    channel.onReceiveOrNull { throw QubeException(qubeEventToMessage(it)) }
                }
            } finally {
                // TODO - Release connection to Qube
                getDag.cancel()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            LOG.error("Build failure", e)
            InternalError(e)
        }
    }

    private fun transformQuartyResult(it: QuartyResult) = when (it) {
        is QuartyResult.Success -> Success(it.dag)
        is QuartyResult.Failure -> UserError(it.log)
    }

    private fun qubeEventToMessage(it: QubeEvent?) = when (it) {
        is ReadyEvent -> "Unexpected Qube event"
        is ErrorEvent -> "Qube reported error: ${it.message}"
        null -> "Unexpected Qube channel closure"
    }

    private fun getDagAsync(hostname: String, trigger: TriggerDetails) = async(CommonPool) {
        val token = github.getAccessTokenAsync(trigger.installationId).awaitWrapped("acquiring access token from GitHub")

        val cloneUrl = URIBuilder(trigger.cloneUrl).apply { userInfo = "x-access-token:${token.token}" }.build()
        quartyBuilder(hostname).getDag(cloneUrl, trigger.ref).awaitWrapped("communicating with Quarty")
    }

    private suspend fun awaitHostname(channel: ReceiveChannel<QubeEvent>): String {
        val event = channel.receive()
        return when (event) {
            is ReadyEvent -> event.hostname
            is ErrorEvent -> throw QubeException("Qube reported error before container created: ${event.message}")
        }
    }

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = try {
        await()
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: Exception) {
        throw RuntimeException("Error while ${action}", e)
    }

    class QubeException(message: String) : RuntimeException(message)
}
