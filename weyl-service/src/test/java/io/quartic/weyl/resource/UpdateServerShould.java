package io.quartic.weyl.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.geofence.GeofenceId;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geofence.ViolationId;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.message.AlertMessage;
import io.quartic.weyl.message.GeofenceGeometryUpdateMessage;
import io.quartic.weyl.message.GeofenceViolationsUpdateMessage;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.Collections;
import java.util.Optional;

import static com.google.common.collect.Sets.newHashSet;
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
    public void send_geofence_geometry_update() throws Exception {
        final Geometry geometry = new GeometryFactory().createPoint(new Coordinate(1.0, 2.0));
        final io.quartic.weyl.core.model.Feature feature = ImmutableFeature.of(
                "123",
                FeatureId.of("456"),
                geometry,
                Collections.emptyMap()
        );

        server.onGeometryChange(ImmutableList.of(feature));

        verifyMessage(GeofenceGeometryUpdateMessage.of(FeatureCollection.of(ImmutableList.of(
                Feature.of(Optional.of("456"), Optional.of(fromJts(transformer.transform(geometry))), ImmutableMap.of("_id", "456"))
        ))));
    }

    @Test
    public void send_geofence_violation_update_accounting_for_cumulative_changes() throws Exception {
        final GeofenceId geofenceIdA = GeofenceId.of("37");
        final GeofenceId geofenceIdB = GeofenceId.of("38");

        server.onViolationBegin(violation(geofenceIdA));
        server.onViolationBegin(violation(geofenceIdB));
        server.onViolationEnd(violation(geofenceIdA));

        verifyMessage(GeofenceViolationsUpdateMessage.of(ImmutableList.of(geofenceIdA)));
        verifyMessage(GeofenceViolationsUpdateMessage.of(ImmutableList.of(geofenceIdA, geofenceIdB)));
        verifyMessage(GeofenceViolationsUpdateMessage.of(ImmutableList.of(geofenceIdB)));
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

    private Violation violation(GeofenceId geofenceId) {
        return Violation.builder()
                .id(ViolationId.of("1"))
                .featureExternalId("42")
                .geofenceId(geofenceId)
                .message("Hmmm")
                .build();
    }
}
