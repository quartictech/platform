package io.quartic.bild

import io.fabric8.kubernetes.api.model.Job
import io.quartic.common.application.ConfigurationBase

data class KubernetesConfiguraration(
    val namespace: String,
    val template: Job,
    val numConcurrentJobs: Int,
    val maxFailures: Int,
    val creationTimeoutSeconds: Int,
    val runTimeoutSeconds: Int
)
data class BildConfiguration(
    val kubernetes: KubernetesConfiguraration
) : ConfigurationBase()

