package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
import io.quartic.common.auth.internal.InternalTokenGenerator
import io.quartic.common.auth.internal.InternalUser
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.Dag.DagResult
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.*
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.InvalidDag
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5.UserErrorInfo.OtherException
import io.quartic.eval.database.model.PhaseCompletedV6.Artifact.EvaluationOutput
import io.quartic.eval.database.model.PhaseCompletedV6.Artifact.NodeExecution
import io.quartic.eval.database.model.toDatabaseModel
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Repository
import io.quartic.quarty.api.model.Pipeline
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyRequest.*
import io.quartic.quarty.api.model.QuartyResponse.Complete.Error
import io.quartic.quarty.api.model.QuartyResponse.Complete.Result
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import org.apache.http.client.utils.URIBuilder
import java.net.URI
import java.util.concurrent.CompletableFuture

// TODO - retries
// TODO - timeouts
class Evaluator(
    private val sequencer: Sequencer,
    private val github: GitHubInstallationClient,
    private val rawPopulator: RawPopulator,
    private val tokenGenerator: InternalTokenGenerator,
    private val extractDag: (List<Node>) -> DagResult,
    private val quartyBuilder: (String) -> QuartyProxy
) {
    constructor(
        sequencer: Sequencer,
        github: GitHubInstallationClient,
        rawPopulator: RawPopulator,
        tokenGenerator: InternalTokenGenerator
    ) : this(
        sequencer,
        github,
        rawPopulator,
        tokenGenerator,
        { nodes -> Dag.fromRawValidating(nodes) },
        { h -> QuartyProxy(h) }
    )

    private suspend fun getTriggerType(trigger: BuildTrigger) = when(trigger) {
        is Manual -> trigger.triggerType
        is Automated -> trigger.triggerType
        is GithubWebhook -> TriggerType.EVALUATE
    }

    suspend fun evaluateAsync(build: BuildContext) = async(CommonPool) {
        val triggerType = getTriggerType(build.trigger)

        sequencer.sequence(build) {
            val token: GitHubInstallationAccessToken = phase("Acquiring Git credentials") {
                success(github
                    .accessTokenAsync(build.customer.githubInstallationId)
                    .awaitWrapped("acquiring access token from GitHub"))
            }

            val repo: Repository = phase("Fetching repository details") {
                success(github
                    .getRepositoryAsync(build.customer.githubRepoId, token)
                    .awaitWrapped("fetching repository details from GitHub"))
            }

            quartyBuilder(container.hostname).use { quarty ->
                phase<Unit>("Cloning and preparing repository") {
                    extractResultFrom(quarty, Initialise(cloneUrl(repo.cloneUrl, token), commit(build.trigger))) {
                        success(Unit)
                    }
                }

                val dagResult = phase<DagResult>("Evaluating DAG") {
                    extractResultFrom(quarty, Evaluate()) {
                        extractDagFromPipeline(it)
                    }
                }

                // Only do this for manual launch
                if (triggerType == TriggerType.EXECUTE) {
                    executeNodes(quarty, build.customer, (dagResult as DagResult.Valid).dag)
                }
            }
        }
    }

    private suspend fun SequenceBuilder.executeNodes(quarty: QuartyProxy, customer: Customer, dag: Dag) {
        dag.forEach { node ->
            when (node) {
                is Node.Step -> phase<Unit>("Executing step for dataset [${node.output.fullyQualifiedName}]") {
                    extractResultFrom(quarty, Execute(node.id, customer.namespace, generateToken(customer))) {
                        successWithArtifact(NodeExecution(skipped = false), Unit)
                    }
                }

                is Node.Raw -> phase<Unit>("Acquiring raw data for dataset [${node.output.fullyQualifiedName}]") {
                    val copyOccurred = rawPopulator.populate(customer, node)
                    successWithArtifact(NodeExecution(skipped = !copyOccurred), Unit)
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
            is Error -> userError(OtherException(detail))
        }
    }

    private fun commit(trigger: BuildTrigger) = when (trigger) {
        is GithubWebhook -> trigger.commit
        is Automated -> trigger.branch
        is Manual -> trigger.branch
    }

    // TODO - this is obviously fairly strange.  Longer term, the namespace list is probably more than just the "self" namespace.
    private fun generateToken(customer: Customer) =
        tokenGenerator.generate(InternalUser(customer.namespace, listOf(customer.namespace)))

    private fun cloneUrl(cloneUrl: URI, token: GitHubInstallationAccessToken) =
        URIBuilder(cloneUrl).apply { userInfo = token.urlCredentials() }.build()

    private fun PhaseBuilder<DagResult>.extractDagFromPipeline(raw: Any?): PhaseResult<DagResult> {
        val nodes = parseRawPipeline(raw)
        val dag = extractDag(nodes)
        return when (dag) {
            is DagResult.Valid ->
                successWithArtifact(EvaluationOutput(nodes), dag)
            is DagResult.Invalid ->
                userError(InvalidDag(dag.error, dag.nodes))
        }
    }

    private fun parseRawPipeline(raw: Any?) = try {
        OBJECT_MAPPER.convertValue<Pipeline>(raw!!).nodes.map { it.toDatabaseModel() }
    } catch (e: Exception) {
        LOG.error("Error parsing Quarty response: ${raw}")
        throw EvaluatorException("Error parsing Quarty response", getRootCause(e))
    }

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = cancellable(
        block = { await() },
        onThrow = { throw EvaluatorException("Error while ${action}", it) }
    )

    companion object {
        private val LOG by logger()
    }
}
