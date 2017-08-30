package io.quartic.eval

import io.quartic.common.client.ClientBuilder
import io.quartic.common.coroutines.cancellable
import io.quartic.common.coroutines.use
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.github.GitHubInstallationClient
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.QuartyClient.QuartyResult
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.selects.select
import org.apache.http.client.utils.URIBuilder
import retrofit2.HttpException
import java.time.Instant
import java.util.*
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
    private val quartyBuilder: (String) -> QuartyClient,
    private val uuidGen: () -> UUID
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
        { hostname -> QuartyClient(clientBuilder, "http://${hostname}:${quartyPort}") },
        { UUID.randomUUID() }
    )

    private val threadPool = newFixedThreadPoolContext(2, "database")
    private val LOG by logger()

    data class Output(
        val result: Result,
        val messages: List<QuartyMessage>
    )

    suspend fun evaluateAsync(trigger: TriggerDetails) = async(CommonPool) {
        val startTime = Instant.now()
        val customer = getCustomer(trigger)

        if (customer != null) {
            Thingy(customer).run(trigger, startTime)
        }
    }

    // TODO - rename this class
    private inner class Thingy(private val customer: Customer) {
        private val buildId = uuidGen()

        suspend fun run(details: TriggerDetails, startTime: Instant) {
            val build = insertBuild(customer.id, details, startTime)

            TriggerReceived(details).insert(startTime)

            val success = withContainer { container ->
                asPhase("Evaluating DAG") { phaseId ->
                    val output: Output = getDagAsync(container, details).use { getDag ->
                        select {
                            getDag.onAwait { transformQuartyResult(it) }
                            container.errors.onReceive { throw it }
                        }
                    }

                    insertMessages(phaseId, output)
                    output.result
                }
            }

            notifier.notifyAbout(details, customer, build, success)
        }

        private suspend fun withContainer(block: suspend (QubeContainerProxy) -> Boolean): Boolean {
            val success = try {
                qube.createContainer().use { container ->
                    ContainerAcquired(container.hostname).insert()
                    block(container)
                }
            } catch (e: Exception) {
                false
            }
            (if (success) BuildEvent.BUILD_SUCCEEDED else BuildEvent.BUILD_FAILED).insert()
            return success
        }

        private suspend fun asPhase(description: String, block: suspend (UUID) -> Result): Boolean {
            val phaseId = uuidGen()
            PhaseStarted(phaseId, description).insert()
            val result = try {
                block(phaseId)
            } catch (e: Exception) {
                InternalError(e) // TODO - is this right?
            }
            PhaseCompleted(phaseId, result).insert()
            return (result is Success)
        }

        private suspend fun BuildEvent.insert(time: Instant = Instant.now()) = run(threadPool) {
            database.insertEvent2(
                id = uuidGen(),
                buildId = buildId,
                time = time,
                payload = this
            )
        }

        private suspend fun insertBuild(customerId: CustomerId, trigger: TriggerDetails, startTime: Instant) = run(threadPool) {
            database.insertBuild(buildId, customerId, trigger.branch(), trigger, startTime)
            database.getBuild(buildId)
        }

        private suspend fun insertMessages(phaseId: UUID, output: Output) = run(threadPool) {
            val now = Instant.now()
            output.messages.forEach { message ->
                when (message) {
                    is QuartyMessage.Progress -> LogMessageReceived(phaseId, "progress", message.message).insert(now)
                    is QuartyMessage.Log -> LogMessageReceived(phaseId, message.stream, message.line).insert(now)
                    else -> {}  // The other ones have already been turned into QuartyResults
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

    private suspend fun <T> CompletableFuture<T>.awaitWrapped(action: String) = cancellable(
        block = { await() },
        onThrow = { throw EvaluatorException("Error while ${action}", it) }
    )
}
