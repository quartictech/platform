package io.quartic.mgmt

import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.auth.User
import io.quartic.common.secrets.SecretsCodec.EncryptedSecret
import io.quartic.mgmt.auth.Multimap

data class GithubConfiguration(
    val oauthApiRoot: String = "https://github.com",
    val apiRoot: String = "https://api.github.com",
    val clientId: String,
    val clientSecret: EncryptedSecret,
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
    val bildUrl: String,
    val authorisedNamespaces: Multimap<User, DatasetNamespace> = emptyMap(),
    var github: GithubConfiguration,
    val cookies: CookiesConfiguration,
    val tokenTimeToLiveMinutes: Int = 60
): ConfigurationBase()
