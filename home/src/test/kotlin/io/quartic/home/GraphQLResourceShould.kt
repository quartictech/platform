package io.quartic.home

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import graphql.GraphQL
import graphql.annotations.GraphQLAnnotations
import graphql.schema.GraphQLSchema
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.model.Build
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.home.resource.GraphqlResource
import org.junit.Test
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*

class GraphQLResourceShould {
    val eval = mock<EvalQueryServiceClient> {
        on { getBuildsAsync(any()) } doReturn CompletableFuture.completedFuture(listOf(Build(
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
        )))
    }

    val resource = GraphqlResource(eval)


    @Test
    fun be_regular() {
        val user = User("alex", CustomerId(100))
        val result = resource.execute(user, GraphqlResource.Request( "{ feed { id } }"))
        assertThat(result.errors, equalTo(emptyList()))
    }
}
