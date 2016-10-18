package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.live.LayerSubscription;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.GeometryTransformer;
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
public class UpdateServer implements AlertListener {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateServer.class);
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;
    private LayerStore layerStore;
    private List<LayerSubscription> subscriptions = Lists.newArrayList();
    private Session session;

    public UpdateServer(GeometryTransformer geometryTransformer, ObjectMapper objectMapper) {
        this.geometryTransformer = geometryTransformer;
        this.objectMapper = objectMapper;
    }

    public void setLayerStore(LayerStore layerStore) {
        this.layerStore = layerStore;
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

    @Override
    public void onAlert(AbstractAlert alert) {
        sendMessage(AlertMessage.of(alert));
    }

    private void unsubscribeAll() {
        subscriptions.forEach(layerStore::removeSubscriber);
        subscriptions.clear();
    }

    private void subscribe(LayerId layerId) {
        subscriptions.add(layerStore.addSubscriber(layerId, state -> sendLayerUpdate(layerId, state)));
    }

    private void sendLayerUpdate(LayerId layerId, LayerState state) {
        sendMessage(LayerUpdateMessage.builder()
                .layerId(layerId)
                .schema(state.schema())
                .featureCollection(fromJts(state.featureCollection()))  // TODO: obviously we never want to do this with large static layers
                .feedEvents(state.feedEvents())
                .build()
        );
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();    // TODO
        }
    }

    private FeatureCollection fromJts(Collection<io.quartic.weyl.core.model.Feature> features) {
        return FeatureCollection.of(
                features.stream()
                        .map(this::fromJts)
                        .collect(toList())
        );
    }

    private Feature fromJts(io.quartic.weyl.core.model.Feature f) {
        return Feature.of(
                Optional.of(f.externalId()),
                Optional.of(Utils.fromJts(geometryTransformer.transform(f.geometry()))),
                convertMetadata(f.uid(), f.metadata())
        );
    }

    private static Map<String, Object> convertMetadata(FeatureId featureId, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap(metadata);
        output.put("_id", featureId);  // TODO: eliminate the _id concept
        return output;

    }

}
