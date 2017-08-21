package io.quartic.eval

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import java.net.URI

data class EvalConfiguration(
    val registryUrl: URI,
    val qubeUrl: URI,
    val github: GitHubConfiguration
) : ConfigurationBase() {
    data class GitHubConfiguration(
        val appId: String,
        val apiRootUrl: URI,
        val privateKeyEncrypted: EncryptedSecret
    )
}


