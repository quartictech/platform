package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.*
import io.quartic.eval.model.BuildEvent.Companion.BUILD_FAILED
import io.quartic.eval.model.BuildEvent.Companion.BUILD_SUCCEEDED
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.InternalError
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.QuartyClient.QuartyResult.Failure
import io.quartic.quarty.QuartyClient.QuartyResult.Success
import io.quartic.quarty.model.QuartyMessage
import io.quartic.quarty.model.Step
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class EvaluatorShould {

    @Test
    fun run_multiple_evaluations_concurrently() = runBlocking {
        whenever(registry.getCustomerAsync(null, 5678)).thenReturn(CompletableFuture()) // This one blocks indefinitely
        whenever(registry.getCustomerAsync(null, 7777)).thenReturn(exceptionalFuture())

        val a = evaluator.evaluateAsync(details)
        val b = evaluator.evaluateAsync(details.copy(repoId = 7777))

        b.join()                                                // But we can still complete this one

        assertFalse(a.isCompleted)
    }

    @Test
    fun write_dag_to_db_and_notify_if_everything_is_ok() {
        evaluate()

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to ContainerAcquired("a.b.c"),
            uuid(104) to PhaseStarted(phaseId, "Evaluating DAG"),
            uuid(105) to PhaseCompleted(phaseId, Result.Success(EvaluationOutput(steps))),
            uuid(106) to BUILD_SUCCEEDED
        )

        verify(notifier).notifyAbout(details, customer, build(buildId), true)
        runBlocking { verify(container).close() }
    }

    @Test
    fun write_error_to_db_if_dag_is_invalid() {
        whenever(dagIsValid.invoke(steps)).doReturn(false)

        evaluate()

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to ContainerAcquired("a.b.c"),
            uuid(104) to PhaseStarted(phaseId, "Evaluating DAG"),
            uuid(105) to PhaseCompleted(phaseId, Result.UserError("DAG is invalid")),
            uuid(106) to BUILD_FAILED
        )

        runBlocking { verify(container).close() }
    }

    @Test
    fun write_logs_to_db_if_user_code_failed() {
        val messages = listOf(
            QuartyMessage.Progress("yeah yeah yeah"),
            QuartyMessage.Log("stdout", "some log message"),
            QuartyMessage.Error("badness")  // TODO - move this to a dedicated test to prove this is ignored
        )
        whenever(quarty.getResult(any(), any())).thenReturn(completedFuture(Failure(messages, "badness")))

        evaluate()

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to ContainerAcquired("a.b.c"),
            uuid(104) to PhaseStarted(phaseId, "Evaluating DAG"),
            uuid(105) to LogMessageReceived(phaseId, "progress", "yeah yeah yeah"),
            uuid(106) to LogMessageReceived(phaseId, "stdout", "some log message"),
            uuid(107) to PhaseCompleted(phaseId, Result.UserError("badness")),
            uuid(108) to BUILD_FAILED
        )

        runBlocking { verify(container).close() }
    }

    @Test
    fun do_nothing_more_nor_write_to_db_if_customer_lookup_fails() {
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(qube)
        verifyZeroInteractions(database)    // This is the only case where we *don't* record the result
        verifyZeroInteractions(notifier)
    }

    @Test
    fun do_nothing_more_if_qube_fails_to_create_container() {
        runBlocking {
            whenever(qube.createContainer()).thenThrow(QubeException("Stuff is bad"))
        }

        evaluate()

        val buildId = uuid(100)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to BUILD_FAILED
        )
        verifyZeroInteractions(github)
    }

    @Test
    fun do_nothing_more_if_github_token_request_fails() {
        whenever(github.accessTokenAsync(any())).thenReturn(exceptionalFuture())

        evaluate()

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to ContainerAcquired("a.b.c"),
            uuid(104) to PhaseStarted(phaseId, "Evaluating DAG"),
            uuid(105) to PhaseCompleted(phaseId, InternalError(EvaluatorException("Error while acquiring access token from GitHub"))),
            uuid(106) to BUILD_FAILED
        )
        verifyZeroInteractions(quartyBuilder)
    }

    @Test
    fun cancel_async_behaviour_and_close_container_on_concurrent_qube_error() {
        val containerError = QubeException("Stuff is bad")

        whenever(container.errors).thenReturn(produce(CommonPool) {
            send(containerError)
        })

        val ghFuture = CompletableFuture<GitHubInstallationAccessToken>()
        whenever(github.accessTokenAsync(any())).thenReturn(ghFuture)

        evaluate()

        val buildId = uuid(100)
        val phaseId = uuid(103)
        verifyDatabaseInserts(buildId,
            uuid(101) to TriggerReceived(details),
            uuid(102) to ContainerAcquired("a.b.c"),
            uuid(104) to PhaseStarted(phaseId, "Evaluating DAG"),
            uuid(105) to PhaseCompleted(phaseId, InternalError(containerError)),
            uuid(106) to BUILD_FAILED
        )
        verifyZeroInteractions(quartyBuilder)
        runBlocking { verify(container).close() }
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(details).join()
    }

    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
    }

    private fun verifyDatabaseInserts(buildId: UUID, vararg events: Pair<UUID, BuildEvent>) {
        verify(database).insertBuild(eq(buildId),  eq(customer.id), eq(branch), eq(details))
        events.forEach {
            verify(database).insertEvent2(eq(it.first), eq(buildId), any(), eq(it.second))
        }
    }

    private val customerId = CustomerId(999)

    private val details = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
        repoName = "noob",
        cloneUrl = URI("https://noob.com/foo/bar"),
        ref = "develop",
        commit = "abc123",
        timestamp = Instant.MIN
    )

    private val branch = "develop"

    private val customer = Customer(
        id = customerId,
        githubOrgId = 8765,
        githubRepoId = 5678,
        name = "Noobhole Ltd",
        subdomain = "noobhole",
        namespace = "noobhole"
    )

    private fun build(id: UUID) = Database.BuildRow(
        id = id,
        customerId = customerId,
        branch = branch,
        buildNumber = 100,
        triggerDetails = details
    )

    private val steps = mock<List<Step>>()

    private val registry = mock<RegistryServiceClient> {
        on { getCustomerAsync(null, 5678) } doReturn completedFuture(customer)
    }
    private val container = mock<QubeContainerProxy> {
        on { hostname } doReturn "a.b.c"
        on { errors } doReturn produce(CommonPool) {
            delay(500)  // To prevent closure
        }
    }
    private val qube = mock<QubeProxy> {
        on { runBlocking { createContainer() } } doReturn container
    }
    private val github = mock<GitHubInstallationClient> {
        on { accessTokenAsync(1234) } doReturn completedFuture(GitHubInstallationAccessToken(UnsafeSecret("yeah")))
    }
    private val quarty = mock<QuartyClient> {
        on { getResult(URI("https://x-access-token:yeah@noob.com/foo/bar"), "abc123") } doReturn completedFuture(Success(listOf(
            QuartyMessage.Result(steps)
        ), steps))
    }
    private val quartyBuilder = mock<(String) -> QuartyClient> {
        on { invoke("a.b.c") } doReturn quarty
    }
    private val notifier = mock<Notifier>()
    private val database = mock<Database>()
    private val dagIsValid = mock<(List<Step>) -> Boolean>()
    private val sequencer = Sequencer(qube, database, notifier) { uuid(nextUuid++) }
    private val evaluator = Evaluator(
        sequencer,
        registry,
        github,
        dagIsValid,
        quartyBuilder
    )
    private var nextUuid = 100

    private fun uuid(uuid: Int) = UUID(0, uuid.toLong())

    init {
        whenever(dagIsValid.invoke(steps)).doReturn(true)
        whenever(database.getBuild(any())).thenAnswer { invocation ->
            build(invocation!!.arguments[0] as UUID)
        }
    }
}
