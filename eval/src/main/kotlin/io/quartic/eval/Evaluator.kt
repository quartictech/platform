package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.PhaseResult
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyResult
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
    private val extractDag: (Pipeline) -> Dag?,
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
        { pipeline ->
            try {
                Dag.fromSteps(pipeline.steps)
            } catch (e: Exception) {
                null
            }
        },
        { hostname -> QuartyClient(clientBuilder, "http://${hostname}:${quartyPort}") }
    )

    private val LOG by logger()

    suspend fun evaluateAsync(details: TriggerDetails) = async(CommonPool) {
        val customer = getCustomer(details)

        if (customer != null) {
            sequencer.sequence(details, customer) {
                val token: GitHubInstallationAccessToken = phase("Acquiring Git credentials") {
                    success(github
                        .accessTokenAsync(details.installationId)
                        .awaitWrapped("acquiring access token from GitHub"))
                }

                phase<Unit>("Cloning and preparing repository") {
                    extractPhaseResult(
                        fromQuarty { initAsync(cloneUrl(details, token), details.commit) },
                        { success(Unit) }
                    )
                }

                val dag = phase("Evaluating DAG") {
                    extractPhaseResult(
                        fromQuarty { evaluateAsync() },
                        { pipeline -> extractDagFromPipeline(pipeline) }
                    )
                }

                dag
                    .mapNotNull { it.step }    // TODO - what about raw datasets?
                    .forEach { step ->
                        phase<Unit>("Executing step: ${step.name}") {
                            extractPhaseResult(
                                fromQuarty { executeAsync(step.id, customer.namespace) },
                                { success(Unit) }
                            )
                        }
                    }
            }
        }
    }

    private fun cloneUrl(details: TriggerDetails, token: GitHubInstallationAccessToken) =
        URIBuilder(details.cloneUrl).apply { userInfo = "x-access-token:${token.token.veryUnsafe}" }.build()

    private fun <T, R> PhaseBuilder<R>.extractPhaseResult(result: QuartyResult<T>, block: (T) -> PhaseResult<R>) = when(result) {
        is QuartyResult.Success -> block(result.result)
        is QuartyResult.Failure -> userError(result.detail)
        is QuartyResult.InternalError -> internalError(EvaluatorException(result.details))
    }

    private suspend fun <R> PhaseBuilder<*>.fromQuarty(block: QuartyClient.() -> CompletableFuture<out QuartyResult<R>>) =
        block(quartyBuilder(container.hostname))
            .awaitWrapped("communicating with Quarty")
            .apply { logMessages(this) }

    private suspend fun PhaseBuilder<*>.logMessages(result: QuartyResult<*>) =
        result.messages.forEach { log(it.stream, it.message, it.timestamp) }

    private fun PhaseBuilder<Dag>.extractDagFromPipeline(pipeline: Pipeline): PhaseResult<Dag> {
        val dag = extractDag(pipeline)
        return with(dag) {
            if (dag != null) {
                successWithArtifact(EvaluationOutput(pipeline.steps), dag)
            } else {
                userError("DAG is invalid")     // TODO - we probably want a useful diagnostic message from the DAG validator
            }
        }
    }

    private suspend fun getCustomer(details: TriggerDetails) = cancellable(
        block = { registry.getCustomerAsync(null, details.repoId).await() },
        onThrow = { t ->
            if (t is HttpException && t.code() == 404) {
                LOG.warn("Repo ID ${details.repoId} not found in Registry")
            } else {
                LOG.error("Error while communicating with Registry", t)
            }
            null
        }
    )

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = cancellable(
        block = { await() },
        onThrow = { throw EvaluatorException("Error while ${action}", it) }
    )
}
