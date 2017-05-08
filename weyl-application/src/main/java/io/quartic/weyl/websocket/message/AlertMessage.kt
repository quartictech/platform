package io.quartic.weyl.websocket.message

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.quartic.weyl.core.model.Alert

data class AlertMessage(@JsonUnwrapped val alert: Alert) : SocketMessage
