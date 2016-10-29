package io.quartic.weyl.core.live;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

@ClientEndpoint
public class WebsocketLiveImporter {
    private static final Logger LOG = LoggerFactory.getLogger(WebsocketLiveImporter.class);
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;
    private final ObjectMapper objectMapper;
    private final LayerStore layerStore;
    private final LayerId layerId;
    private final Meter messageRateMeter;

    private WebsocketLiveImporter(URI uri, LayerId layerId, UidGenerator<FeatureId> fidGenerator,
                                  UidGenerator<LiveEventId> eidGenerator, ObjectMapper objectMapper,
                                  LayerStore layerStore, MetricRegistry metrics) {
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
        this.objectMapper = objectMapper;
        this.layerStore = layerStore;
        this.layerId = layerId;

        messageRateMeter = metrics.meter(MetricRegistry.name(WebsocketLiveImporter.class, "messages", "rate"));

        ClientManager clientManager = ClientManager.createClient();
        ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {

            @Override
            public boolean onDisconnect(CloseReason closeReason) {
                LOG.info("disconnecting: {}", closeReason);
                return true;
            }

            @Override
            public boolean onConnectFailure(Exception exception) {
                LOG.info("connection failure: {}", exception);
                return true;
            }

            @Override
            public long getDelay() {
                return 1;
            }
        };
        clientManager.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);

        try {
            clientManager.asyncConnectToServer(this, uri);
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
       LOG.info("connected to websocket for layer {}", layerId);
    }

    @OnClose
    public void onClose(Session session) {
        LOG.info("websocket connection closed for layer {}", layerId);
    }

    @OnMessage
    public void onMessage(String message) {
        messageRateMeter.mark();
        try {
            LiveEvent event = objectMapper.readValue(message, LiveEvent.class);

            Stream<Feature> features = event.featureCollection().isPresent() ?
                    event.featureCollection().get().features().stream() : Stream.empty();

            if (features.allMatch(f -> f.id().isPresent())) {
                LiveImporter importer = new LiveImporter(ImmutableList.of(event), fidGenerator, eidGenerator);
                int numFeatures = layerStore.addToLayer(layerId, importer);
                LOG.info("updated {} features for layerId = {}", numFeatures, layerId);
            }
            else {
                LOG.warn("features missing id in layer {}", layerId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static WebsocketLiveImporter start(URI uri, LayerId layerId, UidGenerator<FeatureId> fidGenerator,
                                              UidGenerator<LiveEventId> eidGenerator, LayerStore layerStore,
                                              ObjectMapper objectMapper, MetricRegistry metrics) {
       return new WebsocketLiveImporter(uri, layerId, fidGenerator, eidGenerator, objectMapper, layerStore, metrics);
    }
}
