package io.quartic.registry.api.model

data class Customer(
    val id: Int,
    val githubOrgId: Int,
    val githubRepoId: Int,
    val name: String,
    val subdomain: String,
    val namespace: String
)
