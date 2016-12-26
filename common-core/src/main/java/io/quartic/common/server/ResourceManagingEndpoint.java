package io.quartic.common.server;

import org.slf4j.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class ResourceManagingEndpoint<T> extends Endpoint {
    private static final Logger LOG = getLogger(ResourceManagingEndpoint.class);
    private static final String RESOURCE = "resource";

    @Override
    public final void onOpen(Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        session.getUserProperties().put(RESOURCE, createResourceFor(session));
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        releaseResource((T) session.getUserProperties().get(RESOURCE));
    }

    protected abstract T createResourceFor(Session session);
    protected abstract void releaseResource(T resource);
}
