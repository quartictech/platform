package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.*
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.PhaseBuilder
import io.quartic.eval.sequencer.Sequencer.SequenceBuilder
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
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
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

    // TODO - test build phase name
    // TODO - test logs

    @Test
    fun produce_success_if_everything_works() {
        evaluate()

        assertThat(result, equalTo(Success(EvaluationOutput(steps)) as Result))
    }

    @Test
    fun produce_user_error_if_dag_is_invalid() {
        whenever(dagIsValid.invoke(steps)).doReturn(false)

        evaluate()

        assertThat(result, equalTo(UserError("DAG is invalid") as Result))
    }

    @Test
    fun produce_user_error_if_user_code_failed() {
        whenever(quarty.getResultAsync(any(), any())).thenReturn(completedFuture(Failure(emptyList(), "badness")))

        evaluate()

        assertThat(result, equalTo(UserError("badness") as Result))
    }

    @Test
    fun do_nothing_if_customer_lookup_failed() {
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        evaluate()

        verifyZeroInteractions(sequencer)
    }

    @Test
    fun throw_error_and_do_nothing_more_if_github_token_request_fails() {
        whenever(github.accessTokenAsync(any())).thenReturn(exceptionalFuture())

        evaluate()

        assertThat(throwable, equalTo(EvaluatorException("Error while acquiring access token from GitHub") as Throwable))
        verifyZeroInteractions(quartyBuilder)
    }

    @Test
    fun throw_error_if_quarty_interaction_fails() {
        whenever(quarty.getResultAsync(any(), any())).thenReturn(exceptionalFuture())

        evaluate()

        assertThat(throwable, equalTo(EvaluatorException("Error while communicating with Quarty") as Throwable))
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(details).join()
    }

    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
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

    private val customer = Customer(
        id = customerId,
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
    private val quartyContainer = mock<QubeContainerProxy> {
        on { hostname } doReturn "a.b.c"
        on { errors } doReturn produce(CommonPool) {
            delay(500)  // To prevent closure
        }
    }
    private val github = mock<GitHubInstallationClient> {
        on { accessTokenAsync(1234) } doReturn completedFuture(GitHubInstallationAccessToken(UnsafeSecret("yeah")))
    }
    private val quarty = mock<QuartyClient> {
        on { getResultAsync(URI("https://x-access-token:yeah@noob.com/foo/bar"), "abc123") } doReturn completedFuture(Success(listOf(
            QuartyMessage.Result(steps)
        ), steps))
    }
    private val quartyBuilder = mock<(String) -> QuartyClient> {
        on { invoke("a.b.c") } doReturn quarty
    }
    private val dagIsValid = mock<(List<Step>) -> Boolean>()

    private val sequencer = mock<Sequencer>()
    private val sequenceBuilder = mock<SequenceBuilder>()
    private val phaseBuilder = mock<PhaseBuilder> {
        on { container } doReturn quartyContainer
    }

    private val evaluator = Evaluator(
        sequencer,
        registry,
        github,
        dagIsValid,
        quartyBuilder
    )

    private lateinit var result: Result
    private lateinit var throwable: Throwable

    init {
        whenever(dagIsValid.invoke(steps)).doReturn(true)

        // Mock the Sequencer DSL
        runBlocking {
            whenever(sequencer.sequence(eq(details), eq(customer), any())).then { invocation ->
                runBlocking {
                    (invocation.getArgument<suspend SequenceBuilder.() -> Unit>(2))(sequenceBuilder)
                }
                Unit
            }

            whenever(sequenceBuilder.phase(any(), any())).then { invocation ->
                runBlocking {
                    try {
                        result = (invocation.getArgument<suspend PhaseBuilder.() -> Result>(1))(phaseBuilder)
                    } catch (t: Throwable) {
                        throwable = t
                    }
                }
                Unit
            }
        }
    }
}
