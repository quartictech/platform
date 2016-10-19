package io.quartic.weyl.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.message.AlertMessage;
import io.quartic.weyl.message.GeofenceUpdateMessage;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.Optional;

import static io.quartic.weyl.core.geojson.Utils.fromJts;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static org.mockito.Mockito.*;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryTransformer transformer = webMercatortoWgs84();
    private final UpdateServer server = new UpdateServer(transformer, objectMapper);

    @Before
    public void setUp() throws Exception {
        server.onOpen(session, mock(EndpointConfig.class));
    }

    @Test
    public void send_geofence_update() throws Exception {
        final Geometry geometry = new GeometryFactory().createPoint(new Coordinate(1.0, 2.0));

        server.onGeometryChange(ImmutableList.of(geometry));

        verifyMessage(GeofenceUpdateMessage.of(FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.empty(), Optional.of(fromJts(transformer.transform(geometry))), ImmutableMap.of())
        ))));
    }

    @Test
    public void send_alert() throws Exception {
        final Alert alert = Alert.of("foo", "bar");
        server.onAlert(alert);

        verifyMessage(AlertMessage.of(alert));
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(objectMapper.writeValueAsString(expected));
    }
}
