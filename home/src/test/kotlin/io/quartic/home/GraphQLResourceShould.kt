package io.quartic.home

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.model.Build
import io.quartic.eval.api.model.BuildEvent
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.github.GitHub
import io.quartic.home.resource.GraphQLResource
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
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
    val events = listOf(
        BuildEvent(Instant.now())
    )

    val eval = mock<EvalQueryServiceClient> {
        on { getBuildsAsync(any()) } doReturn CompletableFuture.completedFuture(builds)
        on { getBuildAsync(any(), any()) } doReturn CompletableFuture.completedFuture(builds[0])
        on { getBuildEventsAsync(any(), any()) } doReturn CompletableFuture.completedFuture(events)
    }

    val github = mock<GitHub>()

    val resource = GraphQLResource(eval, github)
    val user = User("alex", CustomerId(100))

    @Test
    fun list_builds() {
        val result = resource.execute(user, GraphQLResource.Request( """
            {
                feed { type, id, time, status, number }
            }"""))

        assertThat(result.errors, equalTo(emptyList()))
        val data = result.data as Map<*, *>
        val feed = data.get("feed") as List<*>
        assertThat(feed.size, equalTo(2))
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
                        ... on Default {
                            time
                        }
                    }
                }
            }""",
            mapOf("buildNumber" to 100)
            ))

        assertThat(result.errors.size, equalTo(0))
    }
}
