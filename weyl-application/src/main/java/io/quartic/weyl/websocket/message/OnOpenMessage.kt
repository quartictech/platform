package io.quartic.weyl.websocket.message

import io.quartic.weyl.WeylConfiguration.MapConfig

data class OnOpenMessage(val config: MapConfig) : SocketMessage
