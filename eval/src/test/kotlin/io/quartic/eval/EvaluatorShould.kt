package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.eval.apis.Database.BuildResult.InternalError
import io.quartic.eval.apis.Database.BuildResult.UserError
import io.quartic.eval.apis.QuartyClient
import io.quartic.eval.apis.QuartyClient.QuartyResult.Failure
import io.quartic.eval.apis.QuartyClient.QuartyResult.Success
import io.quartic.eval.qube.QubeProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.qube.QubeProxy.QubeException
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.qube.api.model.Dag
import io.quartic.qube.api.model.TriggerDetails
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
    fun write_dag_to_db_if_everything_is_ok() {
        evaluate()

        verify(database).writeResult(BuildResult.Success(dag))
        runBlocking { verify(container).close() }
    }

    @Test
    fun write_logs_to_db_if_user_code_failed() {
        whenever(quarty.getDag(any(), any())).thenReturn(completedFuture(Failure("badness")))

        evaluate()

        verify(database).writeResult(UserError("badness"))
        runBlocking { verify(container).close() }
    }

    @Test
    fun do_nothing_more_nor_write_to_db_if_customer_lookup_fails() {
//        registryResponses[0] = exceptionalFuture()
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(qube)
        verifyZeroInteractions(database)    // This is the only case where we *don't* record the result
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

    private fun captureDatabaseResult(): BuildResult {
        val captor = argumentCaptor<BuildResult>()
        verify(database).writeResult(captor.capture())
        return captor.firstValue
    }

    private val details = TriggerDetails(
        type = "github",
        deliveryId = "deadbeef",
        installationId = 1234,
        repoId = 5678,
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

    private val dag = mock<Dag>()

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
        on { accessTokenAsync(1234) } doReturn completedFuture(GitHubInstallationAccessToken("yeah"))
    }
    private val quarty = mock<QuartyClient> {
        on { getDag(URI("https://x-access-token:yeah@noob.com/foo/bar"), "develop") } doReturn completedFuture(Success("stuff", dag))
    }
    private val quartyBuilder = mock<(String) -> QuartyClient> {
        on { invoke("a.b.c") } doReturn quarty
    }
    private val database = mock<Database>()
    private val evaluator = Evaluator(registry, qube, github, database, quartyBuilder)
}
