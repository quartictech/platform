package io.quartic.weyl.core.geofence

import io.quartic.weyl.core.model.Alert
import io.quartic.weyl.core.model.EntityId

data class Violation(
        val entityId: EntityId,
        val geofenceId: EntityId,
        val level: Alert.Level
)
