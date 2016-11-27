package io.quartic.weyl.websocket;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.PingMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.common.uid.UidUtils.stringify;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private final List<ClientStatusMessageHandler> handlers;
    private final AlertProcessor alertProcessor;
    private Session session;

    public UpdateServer(
            AlertProcessor alertProcessor,
            Collection<ClientStatusMessageHandler.Factory> handlerFactories
    ) {
        this.alertProcessor = alertProcessor;
        this.handlers = handlerFactories.stream().map(f -> f.create(this::sendMessage)).collect(toList());
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        this.session = session;
        alertProcessor.addListener(this);
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            final SocketMessage msg = OBJECT_MAPPER.readValue(message, SocketMessage.class);
            if (msg instanceof ClientStatusMessage) {
                ClientStatusMessage csm = (ClientStatusMessage)msg;
                LOG.info("[{}] Subscribed to layers {} + entities {}",
                        session.getId(), stringify(csm.subscribedLiveLayerIds()), stringify(csm.selection().entityIds()));
                handlers.forEach(t -> t.onClientStatusMessage(csm));
            } else if (msg instanceof PingMessage) {
                LOG.info("[{}] Received ping", session.getId());
            } else {
                throw new RuntimeException("Unrecognised type '" + msg.getClass().getCanonicalName() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        alertProcessor.removeListener(this);
        handlers.forEach(t -> {
            try {
                t.close();
            } catch (Exception e) {
                LOG.error("Could not close handler", e);
            }
        });
    }

    @Override
    public void onAlert(Alert alert) {
        sendMessage(AlertMessageImpl.of(alert));
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }
}
