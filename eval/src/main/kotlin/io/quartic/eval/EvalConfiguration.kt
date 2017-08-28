package io.quartic.eval

import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret
import io.quartic.common.db.DatabaseConfiguration
import io.quartic.qube.api.model.ContainerSpec
import java.net.URI

data class EvalConfiguration(
    val registryUrl: URI,
    val heyUrl: URI,
    val qube: QubeConfiguration,
    val github: GitHubConfiguration,
    val database: DatabaseConfiguration
) : ConfigurationBase() {
    data class GitHubConfiguration(
        val appId: String,
        val apiRootUrl: URI,
        val privateKeyEncrypted: EncryptedSecret
    )

    data class QubeConfiguration(
        val url: URI,
        val container: ContainerSpec
    )
}


