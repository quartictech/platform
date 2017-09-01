package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.PhaseResult.*
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.Step
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
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

    suspend fun evaluateAsync(details: TriggerDetails) = async(CommonPool) {
        val customer = getCustomer(details)

        if (customer != null) {
            sequencer.sequence(details, customer) {
                phase("Evaluating DAG") {
                    val token = github
                        .accessTokenAsync(details.installationId)
                        .awaitWrapped("acquiring access token from GitHub")
                    val quartyResult = quartyBuilder(container.hostname)
                        .getPipelineAsync(cloneUrl(details, token), details.commit)
                        .awaitWrapped("communicating with Quarty")

                    logMessages(quartyResult)
                    transformQuartyResult(quartyResult)
                }
            }
        }
    }

    private fun cloneUrl(details: TriggerDetails, token: GitHubInstallationAccessToken) =
        URIBuilder(details.cloneUrl).apply { userInfo = "x-access-token:${token.token.veryUnsafe}" }.build()

    private fun transformQuartyResult(result: QuartyResult?) = when (result) {
        is QuartyResult.Success -> {
            if (dagIsValid(result.result)) {
                SuccessWithArtifact<Void>(EvaluationOutput(result.result))
            } else {
                UserError("DAG is invalid")     // TODO - we probably want a useful diagnostic message from the DAG validator
            }
        }
        is QuartyResult.Failure -> UserError(result.detail)
        null -> InternalError(EvaluatorException("Missing result or failure from Quarty"))
    }

    private suspend fun PhaseBuilder.logMessages(result: QuartyResult?) {
        result?.messages?.forEach { log(it.stream, it.message, it.timestamp) }
    }

    private suspend fun getCustomer(details: TriggerDetails) = cancellable(
        block = { registry.getCustomerAsync(null, details.repoId).await() },
        onThrow = { t ->
            if (t is HttpException && t.code() == 404) {
                LOG.warn("Repo ID ${details.repoId} not found in Registry")
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
