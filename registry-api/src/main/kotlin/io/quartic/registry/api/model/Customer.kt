package io.quartic.registry.api.model

data class Customer(
    val id: Long,
    val githubOrgId: Long,
    val githubRepoId: Long,
    val name: String,
    val subdomain: String,
    val namespace: String
)
