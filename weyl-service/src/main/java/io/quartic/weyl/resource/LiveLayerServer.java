package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.live.LastKnownLocationAndTrackView;
import io.quartic.weyl.core.live.LiveLayer;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerView;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/live-ws/{layerId}")
public class LiveLayerServer {
    private static final Logger LOG = LoggerFactory.getLogger(LiveLayerServer.class);
    private final LiveLayerStore liveLayerStore;
    private final static ObjectMapper OM = new ObjectMapper();

    public LiveLayerServer(LiveLayerStore liveLayerStore) {
        this.liveLayerStore = liveLayerStore;
    }



    @OnOpen
    public void myOnOpen(@PathParam("layerId") String layerId, final Session session) throws IOException {
        liveLayerStore.subscribeView(LayerId.of(layerId), (FeatureCollection featureCollection) -> {
                try {
                    session.getAsyncRemote().sendText(OM.writeValueAsString(featureCollection));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        );

        session.getAsyncRemote().sendText("welcome");
    }

    @OnMessage
    public void myOnMsg(final Session session, String message) {
        session.getAsyncRemote().sendText(message.toUpperCase());
    }

    @OnClose
    public void myOnClose(final Session session, CloseReason cr) {
    }
}
