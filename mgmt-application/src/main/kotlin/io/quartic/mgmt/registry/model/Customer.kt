package io.quartic.mgmt.registry.model

data class Customer(
    val id: Int,
    val githubOrgId: Int,
    val githubRepoId: Int,
    val name: String,
    val subdomain: String,
    val namespace: String
)
