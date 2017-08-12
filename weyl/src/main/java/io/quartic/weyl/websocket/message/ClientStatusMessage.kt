package io.quartic.weyl.websocket.message

import io.quartic.common.geojson.FeatureCollection
import io.quartic.weyl.core.geofence.GeofenceType
import io.quartic.weyl.core.model.Alert
import io.quartic.weyl.core.model.EntityId
import io.quartic.weyl.core.model.LayerId

data class ClientStatusMessage(
        val openLayerIds: List<LayerId>,
        val selection: SelectionStatus,
        val geofence: GeofenceStatus
) : SocketMessage {

    data class SelectionStatus(
        val seqNum: Int,
        val entityIds: List<EntityId>
    )

    data class GeofenceStatus(
        val type: GeofenceType,
        val defaultLevel: Alert.Level,
        val features: FeatureCollection?,
        val layerId: LayerId?,
        val bufferDistance: Double     // TODO: what units?
    )
}
