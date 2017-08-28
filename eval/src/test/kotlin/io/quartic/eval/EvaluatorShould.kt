package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.BuildResult
import io.quartic.eval.apis.BuildResult.InternalError
import io.quartic.eval.apis.BuildResult.UserError
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.eval.apis.Database.EventType
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
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class EvaluatorShould {
    // TODO - distinguish registry 404s

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
    fun write_build_and_phase_to_db() {
        evaluate()

        val captor = argumentCaptor<UUID>()
        verify(database).insertBuild(captor.capture(),  eq(customer.id), eq(details), any())
        verify(database).insertPhase(any(), eq(captor.firstValue), eq("Evaluating DAG"), any())
        runBlocking { verify(container).close() }
    }

    @Test
    fun write_dag_to_db_and_notify_if_everything_is_ok() {
        evaluate()

        verify(database).insertTerminalEvent(any(), any(), eq(EventType.SUCCESS),
            eq(BuildResult.Success(steps)), any())

        verify(notifier).notifyAbout(details, BuildResult.Success(steps))

        runBlocking { verify(container).close() }
    }

    @Test
    fun write_error_to_db_if_dag_is_invalid() {
        whenever(dagIsValid.invoke(steps)).doReturn(false)

        evaluate()

        verify(database).insertTerminalEvent(
            any(),
            any(),
            eq(EventType.USER_ERROR),
            eq(UserError("DAG is invalid")),
            any())
        runBlocking { verify(container).close() }
    }

    @Test
    fun write_logs_to_db_if_user_code_failed() {
        val messages = listOf(
            QuartyMessage.Log("stdout", "some log message"),
            QuartyMessage.Error("badness")
        )
        whenever(quarty.getResult(any(), any())).thenReturn(completedFuture(Failure(messages, "badness")))

        evaluate()

         verify(database).insertEvent(
             any(),
             any(),
             eq(EventType.MESSAGE),
            eq(QuartyMessage.Log("stdout", "some log message")),
            any())

        verify(database).insertEvent(
            any(),
            any(),
            eq(EventType.MESSAGE),
            eq(QuartyMessage.Error("badness")),
            any())

         verify(database).insertTerminalEvent(
             any(),
             any(),
             eq(EventType.USER_ERROR),
             eq(UserError("badness")),
             any())
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

        verifyZeroInteractions(github)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
    }

    @Test
    fun do_nothing_more_if_github_token_request_fails() {
        whenever(github.accessTokenAsync(any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(quartyBuilder)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
    }

    @Test
    fun cancel_async_behaviour_and_close_container_on_concurrent_qube_error() {
        whenever(container.errors).thenReturn(produce(CommonPool) {
            send(QubeException("Stuff is bad"))
        })

        val ghFuture = CompletableFuture<GitHubInstallationAccessToken>()
        whenever(github.accessTokenAsync(any())).thenReturn(ghFuture)

        evaluate()

        verifyZeroInteractions(quartyBuilder)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
        runBlocking { verify(container).close() }
    }


    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(details).join()
    }

    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
    }

    private fun captureDatabaseResult(): Any {
        val captor = argumentCaptor<BuildResult>()
        verify(database).insertTerminalEvent(any(), any(), any(), captor.capture(), any())
        return captor.firstValue
    }

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

    private val customer = Customer(
        id = CustomerId(999),
        githubOrgId = 8765,
        githubRepoId = 5678,
        name = "Noobhole Ltd",
        subdomain = "noobhole",
        namespace = "noobhole"
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
    private val evaluator = Evaluator(registry, qube, github, database, notifier, dagIsValid, quartyBuilder)

    init {
        whenever(dagIsValid.invoke(steps)).doReturn(true)
    }
}
