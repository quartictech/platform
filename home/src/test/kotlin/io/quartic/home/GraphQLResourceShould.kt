package io.quartic.home

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.model.Build
import io.quartic.eval.api.model.ApiBuildEvent
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.github.GitHub
import io.quartic.github.GitHubUser
import io.quartic.home.resource.GraphQLResource
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

class GraphQLResourceShould {
    val builds = listOf(Build(
            id = UUID.randomUUID(),
            customerId = CustomerId(100),
            trigger = BuildTrigger.Manual(
                user = "noob",
                branch = "wat",
                customerId = CustomerId(100),
                timestamp = Instant.now(),
                triggerType = BuildTrigger.TriggerType.EXECUTE
            ),
            status = "wat",
            branch = "wat",
            buildNumber = 100,
            time = Instant.now()
        ),
        Build(
            id = UUID.randomUUID(),
            customerId = CustomerId(100),
            trigger = BuildTrigger.Manual(
                user = "noob",
                branch = "wat",
                customerId = CustomerId(100),
                timestamp = Instant.now(),
                triggerType = BuildTrigger.TriggerType.EXECUTE
            ),
            status = "wat",
            branch = "wat",
            buildNumber = 100,
            time = Instant.now()
        )
        )
    private val events = listOf<ApiBuildEvent>(
        ApiBuildEvent.PhaseCompleted(UUID.randomUUID(), Instant.now(), UUID.randomUUID())
    )

    private val eval = mock<EvalQueryServiceClient> {
        on { getBuildsAsync(any()) } doReturn CompletableFuture.completedFuture(builds)
        on { getBuildAsync(any(), any()) } doReturn CompletableFuture.completedFuture(builds[0])
        on { getBuildEventsAsync(any(), any()) } doReturn CompletableFuture.completedFuture(events)
    }

    private val github = mock<GitHub>() {
        on { user(eq(111)) } doReturn GitHubUser(
            111,
            "bigmo",
            "Big Monad",
            URI.create("http://noob.gif")
        )
    }

    private val resource = GraphQLResource(eval, github)
    private val user = User("111", CustomerId(100))

    @Test
    fun list_builds() {
        val result = resource.execute(user, GraphQLResource.Request( """
            {
                feed { type, id, time, status, number }
            }"""))

        assertThat(result.errors, equalTo(emptyList()))
        val feed = result.data["feed"] as List<Map<String, *>>
        assertThat(feed.size, equalTo(2))
        assertThat(feed[0].keys, equalTo(setOf("type", "id", "time", "status", "number")))
    }

    @Test
    fun fetch_build() {
        val buildNumber = "\$buildNumber"
        val result = resource.execute(user, GraphQLResource.Request( """
            query FetchById($buildNumber: Long) {
                build(number:$buildNumber) {
                    id,
                    number,
                    events {
                        ... on PhaseCompleted {
                            phase_id
                        }
                    }
                }
            }""",
            mapOf("buildNumber" to 100)
            ))

        assertThat(result.errors.size, equalTo(0))
        val data = result.data["build"] as Map<String, *>
        assertThat(data.keys, equalTo(setOf<String>("id", "number", "events")))
    }

    @Test
    fun fetch_profile() {
        val result = resource.execute(user, GraphQLResource.Request(
            "{ profile { name, avatarUrl } }",
            emptyMap()
        ))

        assertThat(result.errors.size, equalTo(0))
        val data: Map<String, Any> = result.data["profile"] as Map<String, Any>
        val expected: Map<String, Any> = mapOf("name" to "Big Monad", "avatarUrl" to "http://noob.gif")
        assertThat(data, equalTo(expected))
    }
}
