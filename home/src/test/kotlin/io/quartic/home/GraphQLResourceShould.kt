package io.quartic.home

import com.nhaarman.mockito_kotlin.*
import io.quartic.common.auth.frontend.FrontendUser
import io.quartic.common.model.CustomerId
import io.quartic.common.test.exceptionalFuture
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.ApiPhaseCompletedResult
import io.quartic.eval.api.model.Build
import io.quartic.eval.api.model.BuildTrigger.Manual
import io.quartic.eval.api.model.BuildTrigger.TriggerType
import io.quartic.github.GitHubClient
import io.quartic.github.GitHubUser
import io.quartic.home.resource.GraphQLResource
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture.completedFuture

class GraphQLResourceShould {
    private val builds = listOf(Build(
            id = UUID.randomUUID(),
            customerId = CustomerId(100),
            trigger = Manual(
                user = "noob",
                branch = "wat",
                customerId = CustomerId(100),
                timestamp = Instant.now(),
                triggerType = TriggerType.EXECUTE
            ),
            status = "wat",
            branch = "wat",
            buildNumber = 100,
            time = Instant.now()
        ),
        Build(
            id = UUID.randomUUID(),
            customerId = CustomerId(100),
            trigger = Manual(
                user = "noob",
                branch = "wat",
                customerId = CustomerId(100),
                timestamp = Instant.now(),
                triggerType = TriggerType.EXECUTE
            ),
            status = "wat",
            branch = "wat",
            buildNumber = 100,
            time = Instant.now()
        )
    )

    private val events = listOf<ApiBuildEvent>(
        ApiBuildEvent.PhaseCompleted(UUID.randomUUID(),
            ApiPhaseCompletedResult.Success(), Instant.now(), UUID.randomUUID()),
        ApiBuildEvent.PhaseCompleted(UUID.randomUUID(),
            ApiPhaseCompletedResult.UserError("noob"), Instant.now(), UUID.randomUUID())

    )

    private val eval = mock<EvalQueryServiceClient> {
        on { getBuildsAsync(any()) } doReturn completedFuture(builds)
        on { getBuildAsync(any(), any()) } doReturn completedFuture(builds[0])
        on { getBuildEventsAsync(any(), any()) } doReturn completedFuture(events)
    }

    private val github = mock<GitHubClient> {
        on { userAsync(eq(111)) } doReturn completedFuture(GitHubUser(
            111,
            "bigmo",
            "Big Monad",
            URI.create("http://noob.gif")
        ))
    }

    private val resource = GraphQLResource(eval, github)
    private val user = FrontendUser("111", CustomerId(100))

    @Test
    fun list_builds() {
        val result = resource.execute(user, buildsRequest)

        assertThat(result.errors, nullValue())
        @Suppress("UNCHECKED_CAST")
        val feed = result.data["feed"] as List<Map<String, *>>
        assertThat(feed.size, equalTo(2))
        assertThat(feed[0].keys, equalTo(setOf("type", "id", "time", "status", "number")))
    }

    @Test
    fun fetch_build() {
        val result = resource.execute(user, buildRequest)

        assertThat(result.errors, nullValue())
        @Suppress("UNCHECKED_CAST")
        val data = result.data["build"] as Map<String, *>
        assertThat(data.keys, equalTo(setOf("id", "number", "events")))
    }

    @Test
    fun fetch_profile() {
        val result = resource.execute(user, profileRequest)

        assertThat(result.errors, nullValue())
        @Suppress("UNCHECKED_CAST")
        val data: Map<String, Any> = result.data["profile"] as Map<String, Any>
        val expected: Map<String, Any> = mapOf("name" to "Big Monad", "avatarUrl" to "http://noob.gif")
        assertThat(data, equalTo(expected))
    }

    @Test
    fun prevent_stack_traces_from_leaking() {
        whenever(eval.getBuildsAsync(any())).doReturn(exceptionalFuture(RuntimeException("Horrendous badness occurred")))

        val result = resource.execute(user, buildsRequest)

        assertThat(result.errors!!.map { it.message }, not(hasItem(containsString("badness"))))
    }

    private val buildsRequest = GraphQLResource.Request("""
        {
            feed { type, id, time, status, number }
        }
    """)

    private val buildRequest = GraphQLResource.Request("""
            query FetchById(${'$'}buildNumber: Long) {
                build(number:${'$'}buildNumber) {
                    id,
                    number,
                    events {
                        ... on PhaseCompleted {
                            phase_id
                            result {
                                ... on UserError { error }
                            }
                        }
                    }
                }
            }""",
        mapOf("buildNumber" to 100)
    )

    private val profileRequest = GraphQLResource.Request(
        "{ profile { name, avatarUrl } }",
        emptyMap()
    )
}
