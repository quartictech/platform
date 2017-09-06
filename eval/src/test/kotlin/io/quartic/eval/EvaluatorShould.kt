package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.TriggerType
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.eval.model.Dag
import io.quartic.eval.model.Dag.Node
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.eval.sequencer.Sequencer.PhaseResult.SuccessWithArtifact
import io.quartic.eval.sequencer.Sequencer.PhaseResult.UserError
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Owner
import io.quartic.github.Repository
import io.quartic.quarty.QuartyClient
import io.quartic.quarty.model.Pipeline
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
import org.hamcrest.Matchers.*
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

        val a = evaluator.evaluateAsync(details, TriggerType.EXECUTE)
        val b = evaluator.evaluateAsync(details.copy(repoId = 7777), TriggerType.EXECUTE)

        b.join()                                                // But we can still complete this one

        assertFalse(a.isCompleted)
    }

    @Test
    fun use_the_correct_phase_descriptions() {
        execute()

        assertThat(sequencer.descriptions, contains(
            "Acquiring Git credentials",
            "Cloning and preparing repository",
            "Evaluating DAG",
            "Executing step: X",
            "Executing step: Y"
        ))
    }

    @Test
    fun quarty_init_before_evaluation() {
        execute()
        inOrder(quarty) {
            verify(quarty).initAsync(any(), any())
            verify(quarty).evaluateAsync()
        }
    }

    // TODO - until we do true streaming, this may lead to non-monotonic events
    @Test
    fun log_quarty_log_events_with_original_timestamps() {
        val instantA = mock<Instant>()
        val instantB = mock<Instant>()

        whenever(quarty.evaluateAsync()).thenReturn(completedFuture(
            Success(
                listOf(
                    QuartyResult.LogEvent("foo", "Hello", instantA),
                    QuartyResult.LogEvent("bar", "World", instantB)
                ),
                pipeline
            )
        ))

        execute()

        // Other messages should be filtered out
        assertThat(sequencer.logs, contains(
            LogInvocation("foo", "Hello", instantA),
            LogInvocation("bar", "World", instantB)
        ))
    }

    @Test
    fun produce_success_if_everything_works() {
        execute()

        assertThat(sequencer.results, hasItem(SuccessWithArtifact(EvaluationOutput(steps), dag)))
    }

    @Test
    fun produce_user_error_if_dag_is_invalid() {
        whenever(extractDag.invoke(pipeline)).doReturn(null as Dag?)

        execute()

        assertThat(sequencer.results, hasItem(UserError<Any>("DAG is invalid")))
    }

    @Test
    fun produce_user_error_if_user_code_failed() {
        whenever(quarty.evaluateAsync()).thenReturn(completedFuture(Failure(emptyList(), "badness")))

        execute()

        assertThat(sequencer.results, hasItem(UserError<Any>("badness")))
    }

    @Test
    fun do_nothing_if_customer_lookup_failed() {
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        execute()

        assertThat(sequencer.numSequences, equalTo(0))
    }

    @Test
    fun throw_error_and_do_nothing_more_if_github_token_request_fails() {
        whenever(github.accessTokenAsync(any())).thenReturn(exceptionalFuture())

        execute()

        assertThat(sequencer.throwables, hasItem(EvaluatorException("Error while acquiring access token from GitHub")))
        verifyZeroInteractions(quartyBuilder)
    }

    @Test
    fun throw_error_if_quarty_interaction_fails() {
        whenever(quarty.evaluateAsync()).thenReturn(exceptionalFuture())

        execute()

        assertThat(sequencer.throwables, hasItem(EvaluatorException("Error while communicating with Quarty")))
    }

    @Test
    fun execute_steps_from_dag_in_order() {
        execute()

        inOrder(quarty) {
            verify(quarty).executeAsync("abc", customerNamespace)
            verify(quarty).executeAsync("def", customerNamespace)
        }
    }

    @Test
    fun evaluate_only() {
        evaluate()

        verify(quarty, times(0)).executeAsync(any(), any())
    }

    @Test
    fun skip_execution_of_raw_datasets() {
        whenever(dag.iterator()).thenReturn(
            listOf(
                Node(mock(), null),     // Raw
                Node(mock(), stepY)
            ).iterator()
        )

        execute()


        verify(quarty, times(1)).executeAsync(any(), any())
        verify(quarty).executeAsync("def", customerNamespace)
    }

    private fun execute() = runBlocking {
        evaluator.evaluateAsync(details, TriggerType.EXECUTE).join()
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(details, TriggerType.EVALUATE).join()
    }


    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
    }

    private val customerNamespace = "raging"
    private val commitId = "abc123"
    private val containerHostname = "a.b.c"
    private val githubToken = GitHubInstallationAccessToken(UnsafeSecret("yeah"))
    private val githubCloneUrl = URI("https://noob.com/foo/bar")
    private val githubCloneUrlWithCreds = URI("https://${githubToken.xAccessToken()}@noob.com/foo/bar")

    private val stepX = mock<Step> {
        on { name } doReturn "X"
        on { id } doReturn "abc"
    }
    private val stepY = mock<Step> {
        on { name } doReturn "Y"
        on { id } doReturn "def"
    }

    private val githubRepoId: Long = 5678
    private val githubInstallationId: Long = 1234
    private val details = BuildTrigger.GithubWebhook(
        deliveryId = "1",
        repoId = githubRepoId,
        installationId = githubInstallationId,
        commit = commitId,
        repoOwner = "noob",
        repoName = "noobery",
        ref = "refs/heads/wat",
        timestamp = Instant.MIN,
        rawWebhook = emptyMap()
    )

    private val repo = Repository(
        id = githubRepoId,
        name = "noobery",
        fullName = "noob/noobery",
        private = true,
        cloneUrl = githubCloneUrl,
        defaultBranch = "master",
        owner = Owner("noob")
    )

    private val customer = mock<Customer> {
        on { namespace } doReturn customerNamespace
        on { githubRepoId } doReturn githubRepoId
        on { githubInstallationId } doReturn githubInstallationId
    }

    private val steps = mock<List<Step>>()
    private val pipeline = Pipeline(steps)
    private val dag = mock<Dag> {
        on { iterator() } doReturn listOf(
            Node(mock(), stepX),
            Node(mock(), stepY)
        ).iterator()
    }

    private val registry = mock<RegistryServiceClient> {
        on { getCustomerAsync(null, 5678) } doReturn completedFuture(customer)
    }

    private val quartyContainer = mock<QubeContainerProxy> {
        on { hostname } doReturn containerHostname
        on { errors } doReturn produce(CommonPool) {
            delay(500)  // To prevent closure
        }
    }

    private val github = mock<GitHubInstallationClient> {
        on { accessTokenAsync(1234) } doReturn completedFuture(githubToken)
        on { getRepositoryAsync(githubRepoId, githubToken) } doReturn completedFuture(repo)
    }

    private val quarty = mock<QuartyClient> {
        on { initAsync(githubCloneUrlWithCreds, commitId) } doReturn completedFuture(Success(emptyList(), Unit))
        on { evaluateAsync() } doReturn completedFuture(Success(emptyList(), pipeline))
        on { executeAsync(any(), any()) } doReturn completedFuture(Success(emptyList(), Unit))
    }

    private val quartyBuilder = mock<(String) -> QuartyClient> {
        on { invoke(containerHostname) } doReturn quarty
    }

    private val extractDag = mock<(Pipeline) -> Dag?> {
        on { invoke(pipeline) } doReturn dag
    }

    private val sequencer = MySequencer()

    private val evaluator = Evaluator(
        sequencer,
        registry,
        github,
        extractDag,
        quartyBuilder
    )

    private data class LogInvocation(val stream: String, val message: String, val timestamp: Instant)

    private inner class MySequencer : Sequencer {
        var numSequences = 0
        val results = mutableListOf<PhaseResult<*>>()
        val throwables = mutableListOf<Throwable>()
        val descriptions = mutableListOf<String>()
        val logs = mutableListOf<LogInvocation>()

        suspend override fun sequence(trigger: BuildTrigger, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
            numSequences++
            block(MySequenceBuilder())
        }

        private inner class MySequenceBuilder : SequenceBuilder {
            suspend override fun <R> phase(description: String, block: suspend PhaseBuilder<R>.() -> PhaseResult<R>): R {
                descriptions += description
                val result = try {
                    block(MyPhaseBuilder())
                } catch (t: Throwable) {
                    throwables += t
                    throw t
                }

                results += result
                return when (result) {
                    is PhaseResult.Success -> result.output
                    is PhaseResult.SuccessWithArtifact -> result.output
                    is PhaseResult.UserError, is PhaseResult.InternalError -> throw RuntimeException("noob")
                }
            }
        }

        private inner class MyPhaseBuilder<R> : PhaseBuilder<R> {
            override val container = quartyContainer

            suspend override fun log(stream: String, message: String, timestamp: Instant) {
                logs += LogInvocation(stream, message, timestamp)
            }
        }
    }
}
