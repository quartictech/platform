package io.quartic.home.graphql

import io.quartic.common.auth.frontend.FrontendUser
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.github.GitHubClient

data class GraphQLContext(
    val user: FrontendUser,
    val eval: EvalQueryServiceClient,
    val github: GitHubClient
)
