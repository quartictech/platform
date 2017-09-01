package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.eval.sequencer.Sequencer.PhaseResult.SuccessWithArtifact
import io.quartic.eval.sequencer.Sequencer.PhaseResult.UserError
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.model.QuartyResult
import io.quartic.quarty.model.QuartyResult.Failure
import io.quartic.quarty.model.QuartyResult.Success
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

    @Test
    fun use_the_correct_phase_description() {
        evaluate()

        runBlocking {
            verify(sequenceBuilder).phase(eq("Evaluating DAG"), any())
        }
    }

    // TODO - until we do true streaming, this may lead to non-monotonic events
    @Test
    fun log_quarty_log_events_with_original_timestamps() {
        val instantA = mock<Instant>()
        val instantB = mock<Instant>()

        whenever(quarty.getPipelineAsync(any(), any())).thenReturn(completedFuture(
            Success(
                listOf(
                    QuartyResult.LogEvent("foo", "Hello", instantA),
                    QuartyResult.LogEvent("bar", "World", instantB)
                ),
                steps
            )
        ))

        evaluate()

        runBlocking {
            verify(phaseBuilder, times(2)).log(any(), any(), any())        // Other messages should be filtered out
            verify(phaseBuilder).log("foo", "Hello", instantA)
            verify(phaseBuilder).log("bar", "World", instantB)
        }
    }

    @Test
    fun produce_success_if_everything_works() {
        evaluate()

        assertThat(result, equalTo(SuccessWithArtifact<Void>(EvaluationOutput(steps)) as PhaseResult))
    }

    @Test
    fun produce_user_error_if_dag_is_invalid() {
        whenever(dagIsValid.invoke(steps)).doReturn(false)

        evaluate()

        assertThat(result, equalTo(UserError("DAG is invalid") as PhaseResult))
    }

    @Test
    fun produce_user_error_if_user_code_failed() {
        whenever(quarty.getPipelineAsync(any(), any())).thenReturn(completedFuture(Failure(emptyList(), "badness")))

        evaluate()

        assertThat(result, equalTo(UserError("badness") as PhaseResult))
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
        whenever(quarty.getPipelineAsync(any(), any())).thenReturn(exceptionalFuture())

        evaluate()

        assertThat(throwable, equalTo(EvaluatorException("Error while communicating with Quarty") as Throwable))
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(details).join()
    }

    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
    }

    private val details = mock<TriggerDetails> {
        on { repoId } doReturn 5678
        on { installationId } doReturn 1234
        on { commit } doReturn "abc123"
        on { cloneUrl } doReturn URI("https://noob.com/foo/bar")
    }

    private val customer = mock<Customer>()

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
        on { getPipelineAsync(URI("https://x-access-token:yeah@noob.com/foo/bar"), "abc123") } doReturn completedFuture(
            Success(emptyList(), steps)
        )
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

    private lateinit var result: PhaseResult
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
                        result = (invocation.getArgument<suspend PhaseBuilder.() -> PhaseResult>(1))(phaseBuilder)
                    } catch (t: Throwable) {
                        throwable = t
                    }
                }
                Unit
            }
        }
    }
}
