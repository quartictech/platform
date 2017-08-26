package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.coroutines.use
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.BuildResult
import io.quartic.eval.database.Database.BuildResult.*
import io.quartic.eval.model.Dag
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.github.GitHubInstallationClient
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.QuartyClient.QuartyResult
import io.quartic.quarty.model.Step
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
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
    private val github: GitHubInstallationClient,
    private val database: Database,
    private val notifier: Notifier,
    private val dagIsValid: (List<Step>) -> Boolean,
    private val quartyBuilder: (String) -> QuartyClient
) {
    constructor(
        registry: RegistryServiceClient,
        qube: QubeProxy,
        github: GitHubInstallationClient,
        database: Database,
        notifier: Notifier,
        clientBuilder: ClientBuilder,
        quartyPort: Int = 8080
    ) : this(registry, qube, github, database, notifier,
        { steps -> Dag.fromSteps(steps).validate() },
        { hostname -> QuartyClient(clientBuilder, "http://${hostname}:${quartyPort}") }
    )

    private val LOG by logger()

    suspend fun evaluateAsync(trigger: TriggerDetails) = async(CommonPool) {
        val customer = getCustomer(trigger)
        if (customer != null) {
            val result = runBuild(trigger)
            database.writeResult(customer.id, result)
            notifier.notifyAbout(trigger, result)
        }
    }

    private suspend fun getCustomer(trigger: TriggerDetails) = cancellable(
        block = { registry.getCustomerAsync(null, trigger.repoId).await() },
        onThrow = { t ->
            if (t is HttpException && t.code() == 404) {
                LOG.warn("Repo ID ${trigger.repoId} not found in Registry")
            } else {
                LOG.error("Error communicating with Registry", t)
            }
            null
        }
    )

    private suspend fun runBuild(trigger: TriggerDetails) = cancellable(
        block = {
            qube.createContainer().use { container ->
                getDagAsync(container, trigger).use { getDag ->
                    select<BuildResult> {
                        getDag.onAwait { transformQuartyResult(it) }
                        container.errors.onReceive { throw it }
                    }
                }
            }
        },
        onThrow = { t ->
            LOG.error("Build failure", t)
            InternalError(t)
        }
    )

    private fun transformQuartyResult(it: QuartyClient.QuartyResult?) = when (it) {
        is QuartyResult.Success -> {
            if (dagIsValid(it.dag)) {
                Success(it.dag)
            } else {
                UserError("DAG is invalid")     // TODO - we probably want a useful diagnostic message from the DAG validator
            }
        }
        is QuartyResult.Failure -> UserError(it.log)
        null -> InternalError(RuntimeException("Missing result or failure from Quarty"))
    }

    private fun getDagAsync(container: QubeContainerProxy, trigger: TriggerDetails) = async(CommonPool) {
        val token = github.accessTokenAsync(trigger.installationId).awaitWrapped("acquiring access token from GitHub")

        val cloneUrl = URIBuilder(trigger.cloneUrl).apply { userInfo = "x-access-token:${token.token.veryUnsafe}" }.build()
        quartyBuilder(container.hostname).getResult(cloneUrl, trigger.commit).awaitWrapped("communicating with Quarty")
    }

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = cancellable(
        block = { await() },
        onThrow = { throw RuntimeException("Error while ${action}", it) }
    )
}
