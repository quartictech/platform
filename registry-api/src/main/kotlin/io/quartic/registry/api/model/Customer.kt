package io.quartic.registry.api.model

import io.quartic.common.model.CustomerId

data class Customer(
    val id: CustomerId,
    val githubOrgId: Long,
    val githubRepoId: Long,
    val name: String,
    val subdomain: String,
    val namespace: String
)
