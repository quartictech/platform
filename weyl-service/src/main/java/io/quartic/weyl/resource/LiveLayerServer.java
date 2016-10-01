package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerSubscription;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.response.LiveLayerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/live-ws")
public class LiveLayerServer {
    private static final Logger LOG = LoggerFactory.getLogger(LiveLayerServer.class);
    private final LiveLayerStore liveLayerStore;
    private final static ObjectMapper OM = new ObjectMapper();
    private Map<LayerId, LiveLayerSubscription> subscriptions = Maps.newHashMap();
    private Session session;

    public LiveLayerServer(LiveLayerStore liveLayerStore) {
        this.liveLayerStore = liveLayerStore;
    }

    @OnOpen
    public void myOnOpen(final Session session) throws IOException {
        this.session = session;
        LOG.info("[{}] Open", session.getId());
    }

    @OnMessage
    public void myOnMsg(String message) {
        try {
            // TODO: replace this with dedicated Jackson-annotated types
            final TypeReference<HashMap<String, String>> typeRef
                    = new TypeReference<HashMap<String,String>>() {};
            final Map<String, String> map = OM.readValue(message, typeRef);

            final String msgType = map.get("type");
            switch (msgType) {
                case "subscribe":
                    subscribe(LayerId.of(map.get("layerId")));
                    break;
                case "unsubscribe":
                    unsubscribe(LayerId.of(map.get("layerId")));
                    break;
                default:
                    throw new RuntimeException("Unrecognised type '" + msgType + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void myOnClose(CloseReason cr) {
        LOG.info("[{}] Close", session.getId());
        subscriptions.keySet().forEach(this::unsubscribe);  // TODO: need to unsubscribe from everything, even if one throws
    }

    private void subscribe(LayerId layerId) {
        if (subscriptions.containsKey(layerId)) {
            throw new RuntimeException("Already subscribed to layerId '" + layerId + "'");
        }
        LOG.info("[{}] Subscribe to {}", session.getId(), layerId);
        subscriptions.put(layerId, liveLayerStore.subscribeView(layerId, featureCollection -> {
            final LiveLayerUpdate update = LiveLayerUpdate.of(layerId, featureCollection);
            try {
                session.getAsyncRemote().sendText(OM.writeValueAsString(update));
            } catch (JsonProcessingException e) {
                e.printStackTrace();    // TODO
            }
        }));
    }

    private void unsubscribe(LayerId layerId) {
        final LiveLayerSubscription subscription = subscriptions.remove(layerId);
        if (subscription == null) {
            throw new RuntimeException("Not subscribed to layerId '" + layerId + "'");
        }
        LOG.info("[{}] Unsubscribe from {}", session.getId(), layerId);
        liveLayerStore.unsubscribeView(subscription);
    }
}
