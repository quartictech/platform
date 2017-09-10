package io.quartic.home

import com.nhaarman.mockito_kotlin.*
import graphql.GraphQL
import graphql.annotations.GraphQLAnnotations
import graphql.schema.GraphQLSchema
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.model.Build
import io.quartic.eval.api.model.BuildEvent
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.home.resource.GraphqlResource
import org.junit.Test
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.mockito.invocation.InvocationOnMock

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
        on { getBuildsAsync(any(), anyOrNull()) } doReturn CompletableFuture.completedFuture(builds)
        on { getBuildEventsAsync(any(), any()) } doReturn CompletableFuture.completedFuture(events)
    }

    val resource = GraphqlResource(eval)
    val user = User("alex", CustomerId(100))

    @Test
    fun list_builds() {
        val result = resource.execute(user, GraphqlResource.Request( """
            {
                feed {
                    id,
                    #events {
                    #    ... on Default {
                    #        time
                    #    }
                    #}
                }
            }"""))

        assertThat(result.errors, equalTo(emptyList()))
        println(result.data)
    }

    @Test
    fun fetch_build() {
        val buildNumber = "\$buildNumber"
        val result = resource.execute(user, GraphqlResource.Request( """
            query FetchById($buildNumber: Long) {
                build(buildNumber: $buildNumber) {
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

        assertThat(result.errors, equalTo(emptyList()))
    }
}
