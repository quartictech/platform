package io.quartic.zeus.model

import java.time.ZonedDateTime

data class Asset(
    val id: AssetId,
    val clazz: String,
    val model: AssetModel,
    val serial: String,
    val purchaseTimestamp: ZonedDateTime,
    val lastInspectionTimestamp: ZonedDateTime,
    val lastInspectionSignoff: String,
    val retirementTimestamp: ZonedDateTime,
    val location: Location,
    val notes: List<Note>,
    val events: List<MaintenanceEvent>
)