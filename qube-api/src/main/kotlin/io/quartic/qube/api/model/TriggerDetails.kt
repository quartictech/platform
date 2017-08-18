package io.quartic.qube.api.model

import java.time.Instant

// TODO - token or whatever
data class TriggerDetails(
    val type: String,
    val deliveryId: String,     // For logging purposes
    val installationId: Long,
    val repoId: Long,
    val cloneUrl: String,
    val ref: String,
    val commit: String,
    val timestamp: Instant
)
