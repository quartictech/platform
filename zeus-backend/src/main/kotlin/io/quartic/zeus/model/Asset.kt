package io.quartic.zeus.model

import java.time.ZonedDateTime

data class Asset(
    val id: AssetId,
    val clazz: String,
    val model: AssetModel,
    val serial: String,
    val purchaseDate: ZonedDateTime,
    val lastInspectionDate: ZonedDateTime,
    val lastInspectionSignoff: String,
    val retirementDate: ZonedDateTime,
    val location: Location,
    val notes: List<Note>,
    val events: List<MaintenanceEvent>
)