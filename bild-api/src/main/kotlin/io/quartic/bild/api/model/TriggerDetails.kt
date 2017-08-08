package io.quartic.bild.api.model

import java.time.Instant

// TODO - token or whatever
data class TriggerDetails(
    val type: String,
    val deliveryId: String,     // For logging purposes
    val installationId: Long,
    val repoId: Long,
    val ref: String,
    val timestamp: Instant
)
