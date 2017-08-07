package io.quartic.bild

import io.fabric8.kubernetes.api.model.Job
import io.quartic.common.application.ConfigurationBase

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
    val privateKey: String
)

data class BildConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val registryUrl: String,
    val github: GitHubConfiguration
) : ConfigurationBase()

