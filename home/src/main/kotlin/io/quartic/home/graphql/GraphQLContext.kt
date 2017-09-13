package io.quartic.home.graphql

import io.quartic.common.auth.User
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.github.GitHub

data class GraphQLContext(
    val user: User,
    val eval: EvalQueryServiceClient,
    val github: GitHub
)