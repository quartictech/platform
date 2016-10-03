package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.live.LiveLayerState;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerSubscription;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class LiveLayerServer {
    private static final Logger LOG = LoggerFactory.getLogger(LiveLayerServer.class);
    private final ObjectMapper objectMapper;
    private final LiveLayerStore liveLayerStore;
    private List<LiveLayerSubscription> subscriptions = Lists.newArrayList();
    private Session session;

    public LiveLayerServer(ObjectMapper objectMapper, LiveLayerStore liveLayerStore, AlertProcessor alertProcessor) {
        this.objectMapper = objectMapper;
        this.liveLayerStore = liveLayerStore;
        alertProcessor.addListener(this::sendAlert);
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        this.session = session;
        LOG.info("[{}] Open", session.getId());
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            final SocketMessage msg = objectMapper.readValue(message, SocketMessage.class);
            if (msg instanceof ClientStatusMessage) {
                ClientStatusMessage csm = (ClientStatusMessage)msg;
                LOG.info("[{}] Subscribed to {}", session.getId(), csm.subscribedLiveLayerIds());
                unsubscribeAll();
                csm.subscribedLiveLayerIds().forEach(this::subscribe);
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
        unsubscribeAll();
    }

    private void unsubscribeAll() {
        subscriptions.forEach(liveLayerStore::removeSubscriber);
        subscriptions.clear();
    }

    private void subscribe(LayerId layerId) {
        subscriptions.add(liveLayerStore.addSubscriber(layerId, state -> sendLayerUpdate(layerId, state)));
    }

    private void sendLayerUpdate(LayerId layerId, LiveLayerState state) {
        sendMessage(LayerUpdateMessage.of(layerId, state));
    }

    private void sendAlert(AbstractAlert alert) {
        sendMessage(AlertMessage.of(alert));
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();    // TODO
        }
    }
}
