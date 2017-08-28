package io.quartic.qube

import io.fabric8.kubernetes.api.model.Pod
import io.quartic.common.application.ConfigurationBase
import io.quartic.common.db.DatabaseConfiguration

data class KubernetesConfiguraration(
    val namespace: String,
    val podTemplate: Pod,
    val numConcurrentJobs: Int,
    val jobTimeoutSeconds: Long,
    val enable: Boolean,
    val deletePods: Boolean = true
)

data class QubeConfiguration(
    val kubernetes: KubernetesConfiguraration,
    val database: DatabaseConfiguration,
    val websocketPort: Int
) : ConfigurationBase()
