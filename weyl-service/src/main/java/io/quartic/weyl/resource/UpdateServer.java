package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateServer.class);
    private final ObjectMapper objectMapper;
    private final LiveLayerStore liveLayerStore;
    private List<LayerSubscription> subscriptions = Lists.newArrayList();
    private Session session;

    public UpdateServer(ObjectMapper objectMapper, LiveLayerStore liveLayerStore, AlertProcessor alertProcessor) {
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

    private void sendLayerUpdate(LayerId layerId, LayerState state) {
        sendMessage(LayerUpdateMessage.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(fromJts(state.featureCollection()))
                .feedEvents(state.feedEvents())
                .build()
        );
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

    private static FeatureCollection fromJts(Collection<io.quartic.weyl.core.model.Feature> features) {
        return FeatureCollection.of(features.stream().map(UpdateServer::fromJts).collect(toList()));
    }

    private static Feature fromJts(io.quartic.weyl.core.model.Feature f) {
        return Feature.of(
                Optional.of(f.externalId()),
                Optional.of(Utils.fromJts(f.geometry())),
                convertMetadata(f.uid(), f.metadata())
        );
    }

    private static Map<String, Object> convertMetadata(FeatureId featureId, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap(metadata);
        output.put("_id", featureId);  // TODO: eliminate the _id concept
        return output;
    }

}
