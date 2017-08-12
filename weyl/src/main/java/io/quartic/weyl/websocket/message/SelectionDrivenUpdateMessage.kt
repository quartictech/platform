package io.quartic.weyl.websocket.message

data class SelectionDrivenUpdateMessage(
    val subscriptionName: String,
    val seqNum: Int,
    val data: Any
) : SocketMessage
