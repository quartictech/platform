package io.quartic.weyl.websocket.message

import io.quartic.weyl.MapConfig

data class OnOpenMessage(val config: MapConfig) : SocketMessage
