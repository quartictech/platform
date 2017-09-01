package io.quartic.eval.api.model

import java.net.URI
import java.time.Instant

data class TriggerDetails(
    val type: String,
    val deliveryId: String,     // For logging purposes
    val installationId: Long,
    val repoId: Long,
    val repoFullName: String,
    val repoName: String,
    val repoOwner: String,
    val cloneUrl: URI,
    val ref: String,
    val commit: String,
    val timestamp: Instant
) {
    fun branch(): String = ref.removePrefix("refs/heads/")
}
