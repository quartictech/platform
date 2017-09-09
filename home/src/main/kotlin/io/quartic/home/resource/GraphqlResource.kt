package io.quartic.home.resource

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import graphql.GraphQL
import graphql.GraphQLError
import graphql.annotations.GraphQLAnnotations
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import io.dropwizard.auth.Auth
import io.quartic.common.auth.User
import io.quartic.common.logging.logger
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.home.graphql.Query
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

    private val gql: GraphQL

    init {
        val queryType = GraphQLAnnotations.`object`(Query::class.java)
        val graphQLSchema = GraphQLSchema.newSchema()
            .query(queryType)
            .build()
        println(SchemaPrinter().print(graphQLSchema))
        gql = GraphQL.newGraphQL(graphQLSchema)
            .build()

    }


    @POST
    @Path("/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun execute(@Auth user: User, request: Request): Result {
        val executionResult = gql.execute(request.query, Context(user, eval), request.variables)
        if (executionResult.errors.size > 0) {
            LOG.error("Errors: {}", executionResult.getErrors())
        }

        return Result(executionResult.getData(), executionResult.errors)
    }
}
