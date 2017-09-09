package io.quartic.home.resource

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import graphql.GraphQL
import graphql.GraphQLError
import graphql.annotations.*
import graphql.execution.batched.BatchedExecutionStrategy
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.common.logging.logger
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryServiceClient
import java.lang.reflect.AnnotatedType
import java.time.Instant
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/graphql")
class GraphqlResource(val eval: EvalQueryServiceClient) {
    private val LOG by logger()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Request(
        val query: String,
        val variables: Map<String, Any> = emptyMap()
    )

    data class Result(
        val data: Any?,
        val errors: List<GraphQLError>
    )

    data class Context(
        val user: User,
        val eval: EvalQueryServiceClient
    )

    class InstantTypeFunction : TypeFunction {
        override fun canBuildType(aClass: Class<*>?, annotatedType: AnnotatedType?) = aClass == Instant::class.java

        override fun buildType(typeName: String?, aClass: Class<*>?, annotatedType: AnnotatedType?): GraphQLType {
            return object: GraphQLType {
                override fun getName(): String {
                    return "Instant"
                }

            }
        }

    }

    data class Build(
        @GraphQLField
        @GraphQLName("id")
        val id: String,

        @GraphQLField
        @GraphQLName("number")
        val number: Long,

        @GraphQLField
        @GraphQLName("status")
        val status: String,

        @GraphQLField
        @GraphQLName("time")
        val time: Long,

        @GraphQLField
        @GraphQLName("type")
        val type: String = "build"
    )

    class BuildsFetcher : DataFetcher<List<Build>> {
        override fun get(env: DataFetchingEnvironment?): List<Build> {
            val context = env!!.getContext<Context>()
            return context.eval.getBuildsAsync(context.user.customerId!!).get()
                .map { Build(it.id.toString(), it.buildNumber, it.status, it.time.epochSecond) }
        }
    }

    interface Query {
        @GraphQLField
        @GraphQLName("feed")
        @GraphQLDataFetcher(BuildsFetcher::class)
        fun feed(): List<Build>
    }


    @POST
    @Path("/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun execute(@Auth user: User, request: Request): Result {
        GraphQLAnnotations.register(InstantTypeFunction())
        val queryType = GraphQLAnnotations.`object`(Query::class.java)
        val graphQLSchema = GraphQLSchema.newSchema()
            .query(queryType).build()
        val gql = GraphQL.newGraphQL(graphQLSchema)
            .build()
        val executionResult = gql.execute(request.query, Context(user, eval), request.variables)
        if (executionResult.errors.size > 0) {
            LOG.error("Errors: {}", executionResult.getErrors())
        }
        LOG.info("result: {}", executionResult)

        return Result(executionResult.getData(), executionResult.errors)
    }
}
