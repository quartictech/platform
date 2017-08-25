package io.quartic.home

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import java.net.URI

data class GithubConfiguration(
    val oauthApiRoot: String = "https://github.com",
    val apiRoot: String = "https://api.github.com",
    val clientId: String,
    val clientSecretEncrypted: EncryptedSecret,
    val trampolineUrl: URI,
    val scopes: List<String>,
    val redirectHost: String
)

data class CookiesConfiguration(
    val secure: Boolean,
    val maxAgeSeconds: Int
)

data class HomeConfiguration(
    val catalogueUrl: URI,
    val howlUrl: URI,
    val registryUrl: URI,
    val evalUrl: URI,
    var github: GithubConfiguration,
    val cookies: CookiesConfiguration
): ConfigurationBase()
