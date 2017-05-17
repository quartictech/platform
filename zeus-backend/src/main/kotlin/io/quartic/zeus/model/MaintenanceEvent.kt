package io.quartic.zeus.model

import java.time.ZonedDateTime

data class MaintenanceEvent(
        val type: String,
        val timestamp: ZonedDateTime
)