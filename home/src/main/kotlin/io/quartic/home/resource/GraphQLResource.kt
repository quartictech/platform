package io.quartic.home.resource

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import graphql.ErrorType
import graphql.ExceptionWhileDataFetching
import graphql.GraphQL
import graphql.GraphQLError
import graphql.annotations.GraphQLAnnotations
import graphql.schema.GraphQLSchema
import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.common.logging.logger
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.github.GitHub
import io.quartic.home.graphql.GraphQLContext
import io.quartic.home.graphql.Query
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
class GraphQLResource(val eval: EvalQueryServiceClient, val github: GitHub) {
    private val LOG by logger()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Request(
        val query: String,
        val variables: Map<String, Any> = emptyMap()
    )

    data class Result(
        val data: Map<String, *>,
        val errors: List<GraphQLError>
    )

    private val gql: GraphQL

    init {
        val queryType = GraphQLAnnotations.`object`(Query::class.java)
        val schema = GraphQLSchema.newSchema()
            .query(queryType)
            .build()
        gql = GraphQL.newGraphQL(schema)
            .build()

    }

    @POST
    @Path("/gql")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun execute(@Auth user: User, request: Request): Result {
        val context = GraphQLContext(user, eval, github)
        val executionResult = gql.execute(request.query, context, request.variables)
        if (executionResult.errors.size > 0) {
            LOG.error("Errors: {}", executionResult.errors)
        }

        return Result(executionResult.getData(), sanitiseErrors(executionResult.errors))
    }

    private fun sanitiseErrors(unsanitised: List<GraphQLError>): List<GraphQLError> {
        return unsanitised
            .map { error ->
                if (error is ExceptionWhileDataFetching) {
                    object : GraphQLError {
                        override fun getErrorType() = ErrorType.DataFetchingException
                        override fun getLocations() = null
                        override fun getMessage() = "Internal server error"
                    }
                } else {
                    error

                }
            }
    }
}
