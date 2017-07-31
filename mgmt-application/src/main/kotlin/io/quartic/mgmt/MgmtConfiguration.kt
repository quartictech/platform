package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.application.ConfigurationBase
import io.quartic.mgmt.auth.Multimap
import io.quartic.common.auth.User

data class GithubConfiguration(
    val oauthApiRoot: String = "https://github.com",
    val apiRoot: String = "https://api.github.com",
    val clientId: String,
    val clientSecret: String,
    val allowedOrganisations: Set<String>,
    val trampolineUrl: String,
    val useSecureCookies: Boolean,
    val scopes: List<String>,
    val redirectHost: String,
    val cookieMaxAge: Int
)

data class MgmtConfiguration(
    val catalogueUrl: String,
    val howlUrl: String,
    val authorisedNamespaces: Multimap<User, DatasetNamespace> = emptyMap(),
    var github: GithubConfiguration,
    val tokenTimeToLiveMinutes: Int = 60
): ConfigurationBase()
