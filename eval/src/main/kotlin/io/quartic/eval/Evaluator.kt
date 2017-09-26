package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
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
import io.quartic.eval.pruner.Pruner
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.sequencer.BuildBootstrap.BuildContext
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
    private val extractDag: (List<Node>) -> DagResult = { nodes -> Dag.fromRawValidating(nodes) },
    private val dagPruner: Pruner = Pruner(),
    private val quartyBuilder: (String) -> QuartyProxy = { hostname -> QuartyProxy(hostname) }
) {
    private suspend fun getTriggerType(trigger: BuildTrigger) = when(trigger) {
        is Manual -> trigger.triggerType
        is GithubWebhook -> TriggerType.EVALUATE
    }

    suspend fun evaluateAsync(build: BuildContext) = async(CommonPool) {
        val triggerType = getTriggerType(build.trigger)

        sequencer.sequence(build.trigger, build.build, build.customer) {
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
                    (dagResult as DagResult.Valid)
                    val acceptor = dagPruner.acceptorFor(dagResult.dag)

                    dagResult.dag
                        .forEach { node ->
                            val action = when (node) {
                                is Node.Step -> "Executing step"
                                is Node.Raw -> "Acquiring raw data"
                            }
                            phase<Unit>("${action} for dataset [${node.output.fullyQualifiedName}]") {
                                if (acceptor(node)) {
                                    extractResultFrom(quarty, Execute(node.id, build.customer.namespace)) {
                                        successWithArtifact(NodeExecution(skipped = false), Unit)
                                    }
                                } else {
                                    successWithArtifact(NodeExecution(skipped = true), Unit)
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
            is Error -> userError(OtherException(detail))
        }
    }

    private fun commit(trigger: BuildTrigger) = when (trigger) {
        is GithubWebhook -> trigger.commit
        is Manual -> trigger.branch
    }

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
