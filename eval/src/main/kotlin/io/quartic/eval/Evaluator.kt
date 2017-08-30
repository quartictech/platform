package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.coroutines.use
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.github.GitHubInstallationClient
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.QuartyClient.QuartyResult
import io.quartic.quarty.model.QuartyMessage
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
    private val sequencer: Sequencer,
    private val registry: RegistryServiceClient,
    private val github: GitHubInstallationClient,
    private val dagIsValid: (List<Step>) -> Boolean,
    private val quartyBuilder: (String) -> QuartyClient
) {
    constructor(
        sequencer: Sequencer,
        registry: RegistryServiceClient,
        github: GitHubInstallationClient,
        clientBuilder: ClientBuilder,
        quartyPort: Int = 8080
    ) : this(
        sequencer,
        registry,
        github,
        { steps -> Dag.fromSteps(steps).validate() },
        { hostname -> QuartyClient(clientBuilder, "http://${hostname}:${quartyPort}") }
    )

    private val LOG by logger()

    data class Output(
        val result: Result,
        val messages: List<QuartyMessage>
    )

    suspend fun evaluateAsync(details: TriggerDetails) = async(CommonPool) {
        val customer = getCustomer(details)

        if (customer != null) {
            sequencer.sequence(details, customer) {
                phase("Evaluating DAG") {
                    val output = getDagAsync(container, details).use { getDag ->
                        select<Output> {
                            getDag.onAwait { transformQuartyResult(it) }
                            container.errors.onReceive { throw it }
                        }
                    }

                    output.messages.forEach {
                        when (it) {
                            is QuartyMessage.Progress -> log("progress", it.message)
                            is QuartyMessage.Log -> log(it.stream, it.line)
                            else -> {}  // The other ones have already been turned into QuartyResults
                        }
                    }
                    output.result
                }
            }
        }
    }

    private fun getDagAsync(container: QubeContainerProxy, trigger: TriggerDetails) = async(CommonPool) {
        val token = github.accessTokenAsync(trigger.installationId).awaitWrapped("acquiring access token from GitHub")

        val cloneUrl = URIBuilder(trigger.cloneUrl).apply { userInfo = "x-access-token:${token.token.veryUnsafe}" }.build()
        quartyBuilder(container.hostname).getResult(cloneUrl, trigger.commit).awaitWrapped("communicating with Quarty")
    }

    private fun transformQuartyResult(result: QuartyResult?) = Output(
        when (result) {
            is QuartyResult.Success -> {
                if (dagIsValid(result.result)) {
                    Success(EvaluationOutput(result.result))
                } else {
                    UserError("DAG is invalid")     // TODO - we probably want a useful diagnostic message from the DAG validator
                }
            }
            is QuartyResult.Failure -> UserError(result.detail)
            null -> InternalError(EvaluatorException("Missing result or failure from Quarty"))
        },
        result?.messages ?: emptyList()
    )

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

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = cancellable(
        block = { await() },
        onThrow = { throw EvaluatorException("Error while ${action}", it) }
    )
}
