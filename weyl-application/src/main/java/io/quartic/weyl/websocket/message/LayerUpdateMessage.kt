package io.quartic.weyl.websocket.message

import io.quartic.common.geojson.FeatureCollection
import io.quartic.weyl.core.model.DynamicSchema
import io.quartic.weyl.core.model.LayerId
import io.quartic.weyl.core.model.LayerStats
import io.quartic.weyl.core.model.SnapshotId

data class LayerUpdateMessage(
    val layerId: LayerId,
    val snapshotId: SnapshotId,
    val dynamicSchema: DynamicSchema,
    val stats: LayerStats,
    val featureCollection: FeatureCollection
) : SocketMessage
