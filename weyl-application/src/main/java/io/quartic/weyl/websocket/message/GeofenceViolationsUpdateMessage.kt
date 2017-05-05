package io.quartic.weyl.websocket.message

import io.quartic.weyl.core.model.EntityId

data class GeofenceViolationsUpdateMessage(
    val ids: Set<EntityId>,
    val numInfo: Int,
    val numWarning: Int,
    val numSevere: Int
) : SocketMessage
