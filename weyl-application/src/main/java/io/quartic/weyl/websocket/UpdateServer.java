package io.quartic.weyl.websocket;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.GeofenceListener;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.*;
import org.slf4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static io.quartic.common.uid.UidUtils.stringify;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener, GeofenceListener {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private final FeatureConverter featureConverter;
    private final ObjectMapper objectMapper;
    private final Set<Violation> violations = newLinkedHashSet();
    private final List<ClientStatusMessageHandler> handlers;
    private final GeofenceStore geofenceStore;
    private final AlertProcessor alertProcessor;
    private Session session;

    public UpdateServer(
            GeofenceStore geofenceStore,
            AlertProcessor alertProcessor,
            Collection<ClientStatusMessageHandler.Factory> handlerFactories,
            FeatureConverter featureConverter,
            ObjectMapper objectMapper
    ) {
        this.geofenceStore = geofenceStore;
        this.alertProcessor = alertProcessor;
        this.handlers = handlerFactories.stream().map(f -> f.create(this::sendMessage)).collect(toList());
        this.featureConverter = featureConverter;
        this.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        this.session = session;
        alertProcessor.addListener(this);
        geofenceStore.addListener(this);
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            final SocketMessage msg = objectMapper.readValue(message, SocketMessage.class);
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
        geofenceStore.removeListener(this);
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

    @Override
    public void onViolationBegin(Violation violation) {
        synchronized (violations) {
            violations.add(violation);
            sendViolationsUpdate();
        }
    }

    @Override
    public void onViolationEnd(Violation violation) {
        synchronized (violations) {
            violations.remove(violation);
            sendViolationsUpdate();
        }
    }

    @Override
    public void onGeometryChange(Collection<Feature> features) {
        sendMessage(GeofenceGeometryUpdateMessageImpl.of(featureConverter.toGeojson(features)));
    }

    private void sendViolationsUpdate() {
        sendMessage(GeofenceViolationsUpdateMessageImpl.of(violations.stream().map(v -> v.geofence().feature().entityId()).collect(toList())));
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }
}
