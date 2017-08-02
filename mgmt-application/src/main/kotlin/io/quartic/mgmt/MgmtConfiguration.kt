package io.quartic.mgmt

import io.quartic.common.application.ConfigurationBase

data class GithubConfiguration(
    val oauthApiRoot: String = "https://github.com",
    val apiRoot: String = "https://api.github.com",
    val clientId: String,
    val clientSecret: String,
    val trampolineUrl: String,
    val scopes: List<String>,
    val redirectHost: String
)

data class CookiesConfiguration(
    val secure: Boolean,
    val maxAgeSeconds: Int
)

data class MgmtConfiguration(
    val catalogueUrl: String,
    val howlUrl: String,
    val registryUrl: String,
    var github: GithubConfiguration,
    val cookies: CookiesConfiguration,
    val tokenTimeToLiveMinutes: Int = 60
): ConfigurationBase()
