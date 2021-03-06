package io.quartic.eval

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.auth.internal.InternalTokenGenerator
import io.quartic.common.auth.internal.InternalUser
import io.quartic.common.model.CustomerId
import io.quartic.common.secrets.UnsafeSecret
import io.quartic.common.test.exceptionalFuture
import io.quartic.eval.Dag.DagResult
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.eval.api.model.BuildTrigger.Manual
import io.quartic.eval.api.model.BuildTrigger.TriggerType.EXECUTE
import io.quartic.eval.database.Database
import io.quartic.eval.database.model.PhaseCompletedV8.Node
import io.quartic.eval.database.model.PhaseCompletedV8.UserErrorInfo.InvalidDag
import io.quartic.eval.database.model.PhaseCompletedV8.UserErrorInfo.OtherException
import io.quartic.eval.database.model.PhaseCompletedV8.Artifact.EvaluationOutput
import io.quartic.eval.database.model.PhaseCompletedV8.Artifact.NodeExecution
import io.quartic.eval.database.model.TriggerReceived
import io.quartic.eval.database.model.toDatabaseModel
import io.quartic.eval.quarty.QuartyProxy
import io.quartic.eval.qube.QubeProxy.QubeContainerProxy
import io.quartic.eval.sequencer.BuildInitiator.BuildContext
import io.quartic.eval.sequencer.Sequencer
import io.quartic.eval.sequencer.Sequencer.*
import io.quartic.eval.sequencer.Sequencer.PhaseResult.SuccessWithArtifact
import io.quartic.eval.sequencer.Sequencer.PhaseResult.UserError
import io.quartic.github.GitHubInstallationClient
import io.quartic.github.GitHubInstallationClient.GitHubInstallationAccessToken
import io.quartic.github.Owner
import io.quartic.github.Repository
import io.quartic.quarty.api.model.Pipeline
import io.quartic.quarty.api.model.Pipeline.Dataset
import io.quartic.quarty.api.model.Pipeline.LexicalInfo
import io.quartic.quarty.api.model.Pipeline.Node.Raw
import io.quartic.quarty.api.model.Pipeline.Node.Step
import io.quartic.quarty.api.model.Pipeline.Source.Bucket
import io.quartic.quarty.api.model.QuartyRequest.*
import io.quartic.quarty.api.model.QuartyResponse.Complete.Error
import io.quartic.quarty.api.model.QuartyResponse.Complete.Result
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasItem
import org.junit.Assert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture

class EvaluatorShould {
    @Test
    fun use_the_correct_phase_descriptions() {
        execute()

        assertThat(sequencer.descriptions, contains(
            "Acquiring Git credentials",
            "Fetching repository details",
            "Cloning and preparing repository",
            "Evaluating DAG",
            "Acquiring raw data for dataset [::X]",
            "Executing step for dataset [::Y]",
            "Executing step for dataset [::Z]"
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

        assertThat(sequencer.results, hasItem(SuccessWithArtifact(EvaluationOutput(nodes.map { it.toDatabaseModel() }),
            DagResult.Valid(dag))))
    }

    @Test
    fun produce_user_error_if_dag_is_invalid() {
        whenever(extractDag(any())).doReturn(DagResult.Invalid("Dag is das noob", listOf()))

        execute()

        assertThat(sequencer.results, hasItem(UserError<Any>(InvalidDag("Dag is das noob", listOf()))))
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

        assertThat(sequencer.results, hasItem(UserError<Any>(OtherException("badness"))))
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
    fun produce_node_execution_artifacts_according_to_populator() {
        whenever(runBlocking { populator.populate(any(), eq(rawX.toDatabaseModel() as Node.Raw)) } ).thenReturn(false)

        execute()

        assertThat(sequencer.results, hasItem(SuccessWithArtifact(NodeExecution(skipped = true), Unit)))
    }

    @Test
    fun execute_dag_nodes_in_order_with_unique_token_per_step() {
        execute()

        inOrder(populator, quarty) {
            runBlocking {
                verify(populator).populate(customer, rawX.toDatabaseModel() as Node.Raw)
                verify(quarty).request(eq(Execute("def", customerNamespace, "t1")), any())
                verify(quarty).request(eq(Execute("ghi", customerNamespace, "t2")), any())
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
    fun execute_when_execute_on_push_configured() {
        whenever(customer.executeOnPush).thenReturn(true)
        evaluate()

        runBlocking {
            verify(quarty).request(eq(Execute("def", customerNamespace, "t1")), any())
        }
    }

    private fun execute() = runBlocking {
        evaluator.evaluateAsync(BuildContext(manualTrigger, customer, buildRow)).join()
    }

    private fun evaluate() = runBlocking {
        evaluator.evaluateAsync(BuildContext(webhookTrigger, customer, buildRow)).join()
    }

    private val customerNamespace = "raging"
    private val customerId = CustomerId(100)
    private val commitId = "abc123"
    private val branch = "master"
    private val containerHostname = "a.b.c"
    private val githubToken = GitHubInstallationAccessToken(UnsafeSecret("yeah"))
    private val githubCloneUrl = URI("https://noob.com/foo/bar")
    private val githubCloneUrlWithCreds = URI("https://${githubToken.urlCredentials()}@noob.com/foo/bar")

    private val lexicalInfo = LexicalInfo("whatever", "whatever", "whatever", emptyList())
    private val rawX = Raw(
        id = "abc",
        name = "abc",
        metadata = mapOf(),
        info = lexicalInfo,
        output = Dataset(null, "X"),
        source = Bucket("whatever", "whatever")
    )
    private val stepY = Step(
        id = "def",
        name = "def",
        metadata = mapOf(),
        info = lexicalInfo,
        inputs = emptyList(),
        output = Dataset(null, "Y")
    )
    private val stepZ = Step(
        id = "ghi",
        name = "ghi",
        metadata = mapOf(),
        info = lexicalInfo,
        inputs = emptyList(),
        output = Dataset(null, "Z")
    )

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

    private val manualTrigger = Manual(
        "me",
        Instant.now(),
        customerId,
        "master",
        EXECUTE
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
        on { id } doReturn customerId
        on { namespace } doReturn customerNamespace
        on { githubRepoId } doReturn githubRepoId
        on { githubInstallationId } doReturn githubInstallationId
    }

    private val buildRow = Database.BuildRow(
        UUID.randomUUID(),
        100,
        "develop",
        customer.id,
        "running",
        Instant.MIN,
        TriggerReceived(webhookTrigger.toDatabaseModel())
    )

    private val nodes = listOf(rawX, stepY, stepZ)
    private val pipeline = Pipeline(nodes)
    private val dag = mock<Dag> {
        on { iterator() } doReturn nodes.map{ it.toDatabaseModel() }.iterator()
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

    private val extractDag = mock<(List<Node>) -> DagResult>()

    private val populator = mock<RawPopulator> {
        onGeneric { runBlocking { populate(any(), any()) } } doReturn true
    }

    private val tokenGen = mock<InternalTokenGenerator> {
        on { generate(InternalUser(customerNamespace, listOf(customerNamespace))) } doReturn "t1" doReturn "t2"
    }

    private val sequencer = spy(MySequencer())

    private val evaluator = Evaluator(
        sequencer,
        github,
        populator,
        tokenGen,
        extractDag,
        quartyBuilder
    )

    private inner class MySequencer : Sequencer {
        val results = mutableListOf<PhaseResult<*>>()
        val throwables = mutableListOf<Throwable>()
        val descriptions = mutableListOf<String>()

        suspend override fun sequence(context: BuildContext, block: suspend SequenceBuilder.() -> Unit) {
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

    init {
        whenever(extractDag(nodes.map { it.toDatabaseModel() })).thenReturn(DagResult.Valid(dag))
    }
}
