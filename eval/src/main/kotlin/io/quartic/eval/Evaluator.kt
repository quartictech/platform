@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.PhaseResult
import io.quartic.eval.websocket.WebsocketClient
import io.quartic.eval.websocket.WebsocketClientImpl
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Repository
import io.quartic.quarty.model.Pipeline
import io.quartic.quarty.model.QuartyRequest
import io.quartic.quarty.model.QuartyRequest.*
import io.quartic.quarty.model.QuartyResponse
import io.quartic.quarty.model.QuartyResponse.*
import io.quartic.quarty.model.QuartyResponse.Complete.Error
import io.quartic.quarty.model.QuartyResponse.Complete.Result
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
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
    private val quartyBuilder: (String) -> WebsocketClient<QuartyRequest, QuartyResponse>
) {
    constructor(
        sequencer: Sequencer,
        registry: RegistryServiceClient,
        github: GitHubInstallationClient,
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
        { hostname -> WebsocketClientImpl.create(URI("http://${hostname}:${quartyPort}"), WebsocketClientImpl.NO_RECONNECTION) }
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
                    success(github
                        .getRepositoryAsync(customer.githubRepoId, token)
                        .awaitWrapped("fetching repository details from GitHub"))
                }

                val quarty = QuartyProxy("noobhole", quartyBuilder)    // TODO - hostname

                phase<Unit>("Cloning and preparing repository") {
                    extractPhaseResult(quarty.request(this, Initialise(cloneUrl(repo.cloneUrl, token), commit(trigger)))) {
                        success(Unit)
                    }
                }

                val dag = phase<Dag>("Evaluating DAG") {
                    extractPhaseResult(quarty.request(this, Evaluate())) {
                        extractDagFromPipeline(it)
                    }
                }

                // Only do this for manual launch
                if (triggerType == TriggerType.EXECUTE) {
                    dag
                        .mapNotNull { it.step }    // TODO - what about raw datasets?
                        .forEach { step ->
                            phase<Unit>("Executing step for dataset [${step.outputs[0].fullyQualifiedName}]") {
                                extractPhaseResult(quarty.request(this, Execute(step.id, customer.namespace))) {
                                    success(Unit)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun <R> PhaseBuilder<R>.extractPhaseResult(result: Complete, block: (Any?) -> PhaseResult<R>) = when (result) {
        is Result -> block(result.result)
        is Error -> userError(result.detail)
    }

    private fun commit(trigger: BuildTrigger) = when (trigger) {
        is GithubWebhook -> trigger.commit
        is Manual -> trigger.branch
    }

    private fun cloneUrl(cloneUrl: URI, token: GitHubInstallationAccessToken) =
        URIBuilder(cloneUrl).apply { userInfo = token.urlCredentials() }.build()

    private fun PhaseBuilder<Dag>.extractDagFromPipeline(raw: Any?): PhaseResult<Dag> {
        val pipeline = try {
            OBJECT_MAPPER.convertValue<Pipeline>(raw!!)
        } catch (e: Exception) {
            throw EvaluatorException("Error parsing Quarty response", getRootCause(e))
        }

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
