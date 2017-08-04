package io.quartic.glisten.model

import java.time.Instant

// TODO - token or whatever
data class Notification(
    val type: String,
    val deliveryId: String,     // For logging purposes
    val installationId: Int,
    val repoId: Int,
    val ref: String,
    val timestamp: Instant
)
