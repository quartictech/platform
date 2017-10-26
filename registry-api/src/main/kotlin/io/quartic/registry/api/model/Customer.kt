package io.quartic.registry.api.model

import io.quartic.common.model.CustomerId

data class Customer(
    val id: CustomerId,
    val githubOrgId: Long,
    val githubRepoId: Long,
    val githubInstallationId: Long,
    val name: String,
    val subdomain: String,
    val namespace: String,
    val slackChannel: String? = null,
    val executeOnPush: Boolean = false
)
