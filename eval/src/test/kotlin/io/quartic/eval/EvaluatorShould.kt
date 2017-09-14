package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.database.model.CurrentPhaseCompleted.Artifact.EvaluationOutput
import io.quartic.eval.Dag.Node
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.eval.sequencer.Sequencer.PhaseResult.SuccessWithArtifact
import io.quartic.eval.sequencer.Sequencer.PhaseResult.UserError
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Owner
import io.quartic.github.Repository
import io.quartic.quarty.api.model.Dataset
import io.quartic.quarty.api.model.Pipeline
import io.quartic.quarty.api.model.QuartyRequest.*
import io.quartic.quarty.api.model.QuartyResponse.Complete.Error
import io.quartic.quarty.api.model.QuartyResponse.Complete.Result
import io.quartic.quarty.api.model.Step
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasItem
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

        val a = evaluator.evaluateAsync(webhookTrigger)
        val b = evaluator.evaluateAsync(webhookTrigger.copy(repoId = 7777))

        b.join()                                                // But we can still complete this one

        assertFalse(a.isCompleted)
    }

    @Test
    fun use_the_correct_phase_descriptions() {
        execute()

        assertThat(sequencer.descriptions, contains(
            "Acquiring Git credentials",
            "Fetching repository details",
            "Cloning and preparing repository",
            "Evaluating DAG",
            "Executing step for dataset [::X]",
            "Executing step for dataset [::Y]"
        ))
    }

    @Test
    fun initialise_quarty_before_evaluation() {
        execute()

        inOrder(quarty) {
            runBlocking {
                verify(quarty).request(isA<Initialise>(), any())
                verify(quarty).request(isA<Evaluate>(), any())
            }
        }
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
    fun throw_if_pipeline_is_unparsable() {
        runBlocking {
            whenever(quarty.request(isA<Evaluate>(), any())).doReturn(Result(mapOf("noob" to "hole")))
        }

        execute()

        assertThat(sequencer.throwables, hasItem(EvaluatorException("Error parsing Quarty response")))
    }

    @Test
    fun produce_user_error_if_user_code_failed() {
        runBlocking {
            whenever(quarty.request(isA<Evaluate>(), any())).thenReturn(Error("badness"))
        }

        execute()

        assertThat(sequencer.results, hasItem(UserError<Any>("badness")))
    }

    @Test
    fun do_nothing_if_customer_lookup_failed() {
        whenever(registry.getCustomerAsync(anyOrNull(), any())).thenReturn(exceptionalFuture())

        evaluate()

        runBlocking {
            verify(sequencer, times(0)).sequence(any(), any(), any())
        }
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
        runBlocking {
            whenever(quarty.request(isA<Evaluate>(), any())).thenThrow(EvaluatorException("Noobhole occurred"))
        }

        execute()

        assertThat(sequencer.throwables, hasItem(EvaluatorException("Noobhole occurred")))
    }

    @Test
    fun close_quarty_even_if_something_fails() {
        runBlocking {
            whenever(quarty.request(isA<Evaluate>(), any())).thenThrow(RuntimeException("Sad"))
        }

        execute()

        verify(quarty).close()
    }

    @Test
    fun execute_steps_from_dag_in_order() {
        execute()

        inOrder(quarty) {
            runBlocking {
                verify(quarty).request(eq(Execute("abc", customerNamespace)), any())
                verify(quarty).request(eq(Execute("def", customerNamespace)), any())
            }
        }
    }

    @Test
    fun evaluate_only() {
        evaluate()

        runBlocking {
            verify(quarty, times(0)).request(isA<Execute>(), any())
        }
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

        runBlocking {
            verify(quarty, times(1)).request(isA<Execute>(), any())
            verify(quarty).request(eq(Execute("def", customerNamespace)), any())
        }
    }

    private fun execute() = runBlocking {
        evaluator.evaluateAsync(manualTrigger).join()
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(webhookTrigger).join()
    }


    private fun <R> exceptionalFuture() = CompletableFuture<R>().apply {
        completeExceptionally(RuntimeException("Sad"))
    }

    private val customerNamespace = "raging"
    private val customerId = CustomerId(100)
    private val commitId = "abc123"
    private val branch = "master"
    private val containerHostname = "a.b.c"
    private val githubToken = GitHubInstallationAccessToken(UnsafeSecret("yeah"))
    private val githubCloneUrl = URI("https://noob.com/foo/bar")
    private val githubCloneUrlWithCreds = URI("https://${githubToken.urlCredentials()}@noob.com/foo/bar")

    private val stepX = mock<Step> {
        on { id } doReturn "abc"
        on { outputs } doReturn listOf(Dataset(null, "X"))
    }
    private val stepY = mock<Step> {
        on { id } doReturn "def"
        on { outputs } doReturn listOf(Dataset(null, "Y"))
    }

    private val githubRepoId: Long = 5678
    private val githubInstallationId: Long = 1234
    private val webhookTrigger = BuildTrigger.GithubWebhook(
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

    private val manualTrigger = BuildTrigger.Manual(
        "me",
        Instant.now(),
        customerId,
        "master",
        BuildTrigger.TriggerType.EXECUTE
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
        on { getCustomerByIdAsync(customerId) } doReturn completedFuture(customer)
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

    private val quarty = mock<QuartyProxy> {
        // TODO - get rid of this duplication
        on { runBlocking { request(eq(Initialise(githubCloneUrlWithCreds, commitId)), any()) } } doReturn Result(null)
        on { runBlocking { request(eq(Initialise(githubCloneUrlWithCreds, branch)), any()) } } doReturn Result(null)
        on { runBlocking { request(isA<Evaluate>(), any()) } } doReturn Result(pipeline)
        on { runBlocking { request(isA<Execute>(), any()) } } doReturn Result(null)
    }

    private val quartyBuilder = mock<(String) -> QuartyProxy> {
        on { invoke(containerHostname) } doReturn quarty
    }

    private val extractDag = mock<(Pipeline) -> Dag?> {
        on { invoke(pipeline) } doReturn dag
    }

    private val sequencer = spy(MySequencer())

    private val evaluator = Evaluator(
        sequencer,
        registry,
        github,
        extractDag,
        quartyBuilder
    )

    private inner class MySequencer : Sequencer {
        val results = mutableListOf<PhaseResult<*>>()
        val throwables = mutableListOf<Throwable>()
        val descriptions = mutableListOf<String>()

        suspend override fun sequence(trigger: BuildTrigger, customer: Customer, block: suspend SequenceBuilder.() -> Unit) {
            block(MySequenceBuilder())
        }

        private inner class MySequenceBuilder : SequenceBuilder {
            override val container = quartyContainer

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
            suspend override fun log(stream: String, message: String) {}
        }
    }
}
