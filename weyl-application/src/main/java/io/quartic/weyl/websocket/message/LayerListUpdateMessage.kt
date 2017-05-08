package io.quartic.weyl.websocket.message

import io.quartic.weyl.core.model.LayerId
import io.quartic.weyl.core.model.LayerMetadata
import io.quartic.weyl.core.model.StaticSchema

data class LayerListUpdateMessage(
        val layers: Set<LayerInfo>
) : SocketMessage {
    data class LayerInfo(
        val id: LayerId,
        val metadata: LayerMetadata,
        val staticSchema: StaticSchema,
        val live: Boolean
    )
}
