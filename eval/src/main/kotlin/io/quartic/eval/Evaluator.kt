package io.quartic.eval

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.database.model.CurrentPhaseCompleted.Node
import io.quartic.eval.database.model.toDatabaseModel
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.PhaseResult
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Repository
import io.quartic.quarty.api.model.Pipeline
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyRequest.*
import io.quartic.quarty.api.model.QuartyResponse.Complete.Error
import io.quartic.quarty.api.model.QuartyResponse.Complete.Result
import io.quartic.registry.api.RegistryServiceClient
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
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
    private val extractDag: (List<Node>) -> Dag?,
    private val quartyBuilder: (String) -> QuartyProxy
) {
    constructor(
        sequencer: Sequencer,
        registry: RegistryServiceClient,
        github: GitHubInstallationClient
    ) : this(
        sequencer,
        registry,
        github,
        { nodes ->
            try {
                Dag.fromRaw(nodes)
            } catch (e: Exception) {
                null
            }
        },
        { hostname -> QuartyProxy(hostname) }
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

                quartyBuilder(container.hostname).use { quarty ->
                    phase<Unit>("Cloning and preparing repository") {
                        extractResultFrom(quarty, Initialise(cloneUrl(repo.cloneUrl, token), commit(trigger))) {
                            success(Unit)
                        }
                    }

                    val dag = phase<Dag>("Evaluating DAG") {
                        extractResultFrom(quarty, Evaluate()) {
                            extractDagFromPipeline(it)
                        }
                    }

                    // Only do this for manual launch
                    if (triggerType == TriggerType.EXECUTE) {
                        dag
                            .forEach { node ->
                                val action = when (node) {
                                    is Node.Step -> "Executing step"
                                    is Node.Raw -> "Acquiring raw data"
                                }
                                phase<Unit>("${action} for dataset [${node.output.fullyQualifiedName}]") {
                                    extractResultFrom(quarty, Execute(node.id, customer.namespace)) {
                                        success(Unit)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    private suspend fun <R> PhaseBuilder<R>.extractResultFrom(
        quarty: QuartyProxy,
        request: QuartyRequest,
        block: (Any?) -> PhaseResult<R>
    ) = with(quarty.request(request) { stream, message -> runBlocking { log(stream, message) } }) {
        when(this) {
            is Result -> block(result)
            is Error -> userError(detail)
        }
    }

    private fun commit(trigger: BuildTrigger) = when (trigger) {
        is GithubWebhook -> trigger.commit
        is Manual -> trigger.branch
    }

    private fun cloneUrl(cloneUrl: URI, token: GitHubInstallationAccessToken) =
        URIBuilder(cloneUrl).apply { userInfo = token.urlCredentials() }.build()

    private fun PhaseBuilder<Dag>.extractDagFromPipeline(raw: Any?): PhaseResult<Dag> {
        val nodes = parseRawPipeline(raw)
        val dag = extractDag(nodes)
        return with(dag) {
            if (dag != null) {
                successWithArtifact(EvaluationOutput(nodes), dag)
            } else {
                userError("DAG is invalid")     // TODO - we probably want a useful diagnostic message from the DAG validator
            }
        }
    }

    private fun parseRawPipeline(raw: Any?) = try {
        OBJECT_MAPPER.convertValue<Pipeline>(raw!!).nodes.map { it.toDatabaseModel() }
    } catch (e: JsonMappingException) {
        LOG.error("Invalid JSON: {}", raw)
        throw e
    } catch (e: Exception) {
        LOG.error("Invalid JSON: {}", raw)
        throw EvaluatorException("Error parsing Quarty response", getRootCause(e))
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
