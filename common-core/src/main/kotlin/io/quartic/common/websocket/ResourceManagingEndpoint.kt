package io.quartic.common.websocket

import io.quartic.common.logging.logger
import javax.websocket.CloseReason
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.Session

// TODO: use delegation rather than abstract class
abstract class ResourceManagingEndpoint<T> : Endpoint() {
    private val LOG by logger()

    final override fun onOpen(session: Session, config: EndpointConfig) {
        LOG.info("[{}] Open", session.id)
        session.userProperties.put(RESOURCE, createResourceFor(session))
    }

    @Suppress("UNCHECKED_CAST")
    final override fun onClose(session: Session?, closeReason: CloseReason?) {
        LOG.info("[{}] Close", session!!.id)
        releaseResource(session.userProperties[RESOURCE] as T)
    }

    protected abstract fun createResourceFor(session: Session): T
    protected abstract fun releaseResource(resource: T)

    companion object {
        private val RESOURCE = "resource"
    }
}