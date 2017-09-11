package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.PhaseResult
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Repository
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyResult
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import org.apache.http.client.utils.URIBuilder
import retrofit2.HttpException
import java.net.URI
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

    private suspend fun getTriggerType(trigger: BuildTrigger) = when(trigger) {
        is Manual -> trigger.triggerType
        is GithubWebhook -> TriggerType.EVALUATE
    }

    suspend fun evaluateAsync(trigger: BuildTrigger) = async(CommonPool) {
        val triggerType = getTriggerType(trigger)
        val customer = getCustomer(trigger)

        if (customer != null) {
            sequencer.sequence(trigger, customer) {
                val token: GitHubInstallationAccessToken = phase("Acquiring Git credentials") {
                    success(github
                        .accessTokenAsync(customer.githubInstallationId)
                        .awaitWrapped("acquiring access token from GitHub"))
                }

                val repo: Repository = phase("Fetching repository details") {
                    success(github.getRepositoryAsync(customer.githubRepoId, token)
                        .awaitWrapped("fetching repository details from GitHub"))
                }

                phase<Unit>("Cloning and preparing repository") {
                    extractPhaseResult(
                        fromQuarty { initAsync(cloneUrl(repo.cloneUrl, token), commit(trigger)) },
                        { success(Unit) }
                    )
                }

                val dag = phase("Evaluating DAG") {
                    extractPhaseResult(
                        fromQuarty { evaluateAsync() },
                        { pipeline -> extractDagFromPipeline(pipeline) }
                    )
                }

                // Only do this for manual launch
                if (triggerType == TriggerType.EXECUTE) {
                    dag
                        .mapNotNull { it.step }    // TODO - what about raw datasets?
                        .forEach { step ->
                            phase<Unit>("Executing step for dataset [${step.outputs[0].fullyQualifiedName}]") {
                                extractPhaseResult(
                                    fromQuarty { executeAsync(step.id, customer.namespace) },
                                    { success(Unit) }
                                )
                            }
                        }
                }
            }
        }
    }

    private fun commit(trigger: BuildTrigger) = when (trigger) {
        is GithubWebhook -> trigger.commit
        is Manual -> trigger.branch
    }

    private fun cloneUrl(cloneUrl: URI, token: GitHubInstallationAccessToken) =
        URIBuilder(cloneUrl).apply { userInfo = token.urlCredentials() }.build()

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

    private suspend fun getCustomer(trigger: BuildTrigger) = cancellable(
        block = {
            when (trigger) {
                is GithubWebhook ->
                    registry.getCustomerAsync(null, trigger.repoId)
                is Manual ->
                    registry.getCustomerByIdAsync(trigger.customerId)
            }.await()
        },
        onThrow = { t ->
            if (t is HttpException && t.code() == 404) {
                when (trigger) {
                    is GithubWebhook ->
                        LOG.warn("No customer found for webhook (repoId = ${trigger.repoId})")
                    is Manual ->
                        LOG.warn("No customer found for manual trigger (customerId = ${trigger.customerId})")
                }
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
