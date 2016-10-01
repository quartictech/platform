package io.quartic.weyl.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.live.LiveLayerSubscription;
import io.quartic.weyl.core.model.LayerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/live-ws/{layerId}")
public class LiveLayerServer {
    private static final Logger LOG = LoggerFactory.getLogger(LiveLayerServer.class);
    private final LiveLayerStore liveLayerStore;
    private final static ObjectMapper OM = new ObjectMapper();
    private LiveLayerSubscription subscription = null;

    public LiveLayerServer(LiveLayerStore liveLayerStore) {
        this.liveLayerStore = liveLayerStore;
    }

    @OnOpen
    public void myOnOpen(@PathParam("layerId") String layerId, final Session session) throws IOException {
        this.subscription = liveLayerStore.subscribeView(LayerId.of(layerId), (FeatureCollection featureCollection) -> {
                try {
                    session.getAsyncRemote().sendText(OM.writeValueAsString(featureCollection));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        );
    }

    @OnMessage
    public void myOnMsg(final Session session, String message) {
        // Nothing atm
    }

    @OnClose
    public void myOnClose(final Session session, CloseReason cr) {
        if (subscription != null) {
            liveLayerStore.unsubscribeView(subscription);
        }
    }
}
