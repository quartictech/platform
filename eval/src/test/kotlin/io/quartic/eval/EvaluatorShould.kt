package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.eval.apis.Database
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.eval.apis.Database.BuildResult.*
import io.quartic.eval.apis.GitHubClient
import io.quartic.eval.apis.QuartyClient
import io.quartic.eval.apis.QuartyClient.QuartyResult
import io.quartic.eval.apis.QubeProxy
import io.quartic.eval.apis.QubeProxy.QubeEvent.ErrorEvent
import io.quartic.eval.apis.QubeProxy.QubeEvent.ReadyEvent
import io.quartic.github.GithubInstallationClient.GitHubInstallationAccessToken
import io.quartic.qube.api.model.Dag
import io.quartic.qube.api.model.TriggerDetails
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class EvaluatorShould {
    // TODO - test that we're not wrapping CancellationExceptions
    // TODO - distinguish registry 404s

    @Test
    fun write_dag_to_db_if_everything_is_ok() {
        evaluate()

        verify(database).writeResult(Success(dag))
    }

    @Test
    fun write_logs_to_db_if_user_code_failed() {
        whenever(quarty.getDag(any(), any())).thenReturn(completedFuture(QuartyResult.Failure("badness")))

        evaluate()

        verify(database).writeResult(UserError("badness"))
    }

    @Test
    fun do_nothing_more_nor_write_to_db_if_customer_lookup_fails() {
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(qube)
        verifyZeroInteractions(database)    // This is the only case where we *don't* record the result
    }

    @Test
    fun do_nothing_more_if_qube_fails_to_create_container() {
        whenever(qube.enqueue()).thenReturn(produce(CommonPool) {
            send(ErrorEvent("Stuff is bad"))
        })

        evaluate()

        verifyZeroInteractions(github)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
    }

    @Test
    fun do_nothing_more_if_github_token_request_fails() {
        whenever(github.getAccessTokenAsync(any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(quartyBuilder)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
    }

    @Test
    fun cancel_async_behaviour_on_concurrent_qube_error() {
        whenever(qube.enqueue()).thenReturn(produce(CommonPool) {
            send(ReadyEvent("a.b.c"))
            send(ErrorEvent("Stuff is bad"))
        })

        val ghFuture = CompletableFuture<GitHubInstallationAccessToken>()
        whenever(github.getAccessTokenAsync(any())).thenReturn(ghFuture)

        evaluate()

        verifyZeroInteractions(quartyBuilder)
        assertThat(captureDatabaseResult(), instanceOf(InternalError::class.java))
    }


    private fun evaluate() = runBlocking {
        evaluator.evaluate(details)
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
    private val qube = mock<QubeProxy> {
        on { enqueue() } doReturn produce(CommonPool) {
            send(ReadyEvent("a.b.c"))
            delay(500)  // To prevent closure
        }
    }
    private val github = mock<GitHubClient> {
        on { getAccessTokenAsync(1234) } doReturn completedFuture(GitHubInstallationAccessToken("yeah"))
    }
    private val quarty = mock<QuartyClient> {
        on { getDag(URI("https://x-access-token:yeah@noob.com/foo/bar"), "develop") } doReturn completedFuture(QuartyResult.Success("stuff", dag))
    }
    private val quartyBuilder = mock<(String) -> QuartyClient> {
        on { invoke("a.b.c") } doReturn quarty
    }
    private val database = mock<Database>()
    private val evaluator = Evaluator(registry, qube, github, database, quartyBuilder)
}