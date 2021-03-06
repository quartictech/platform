package io.quartic.eval

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.db.DatabaseConfiguration
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.qube.api.model.PodSpec
import java.net.URI

data class EvalConfiguration(
    val registryUrl: URI,
    val heyUrl: URI,
    val catalogueUrl: URI,
    val howlUrl: URI,
    val homeUrlFormat: String,
    val qube: QubeConfiguration,
    val github: GitHubConfiguration,
    val database: DatabaseConfiguration,
    val auth: AuthConfiguration
) : ConfigurationBase() {
    data class GitHubConfiguration(
        val appId: String,
        val apiRootUrl: URI,
        val privateKeyEncrypted: EncryptedSecret
    )

    data class QubeConfiguration(
        val url: URI,
        val pod: PodSpec
    )

    data class AuthConfiguration(
        val timeToLiveSeconds: Int,
        val signingKeyEncryptedBase64: EncryptedSecret
    )
}


