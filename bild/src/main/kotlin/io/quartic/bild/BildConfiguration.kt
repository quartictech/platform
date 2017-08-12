package io.quartic.bild

import io.dropwizard.db.DataSourceFactory
import io.fabric8.kubernetes.api.model.Job
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.secrets.EncryptedSecret

data class KubernetesConfiguraration(
    val namespace: String,
    val template: Job,
    val numConcurrentJobs: Int,
    val maxFailures: Int,
    val creationTimeoutSeconds: Int,
    val runTimeoutSeconds: Int,
    val backChannelEndpoint: String,
    val enable: Boolean
)

data class GitHubConfiguration(
    val appId: String,
    val apiRootUrl: String,
    val privateKeyEncrypted: EncryptedSecret
)

data class BildConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val database: DataSourceFactory,
    val registryUrl: String,
    val github: GitHubConfiguration
) : ConfigurationBase()

