package io.quartic.home

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret

data class GithubConfiguration(
    val oauthApiRoot: String = "https://github.com",
    val apiRoot: String = "https://api.github.com",
    val clientId: String,
    val clientSecretEncrypted: EncryptedSecret,
    val trampolineUrl: String,
    val scopes: List<String>,
    val redirectHost: String
)

data class CookiesConfiguration(
    val secure: Boolean,
    val maxAgeSeconds: Int
)

data class HomeConfiguration(
    val catalogueUrl: String,
    val howlUrl: String,
    val registryUrl: String,
    val bildUrl: String,
    var github: GithubConfiguration,
    val cookies: CookiesConfiguration,
    val tokenTimeToLiveMinutes: Int = 60
): ConfigurationBase()
