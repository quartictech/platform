package io.quartic.weyl.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import io.quartic.geojson.FeatureCollection;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.geojson.FeatureImpl;
import io.quartic.geojson.PointImpl;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertImpl;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geofence.ViolationImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.*;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.common.serdes.ObjectMappers.encode;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final AlertProcessor alertProcessor = mock(AlertProcessor.class);
    private final ClientStatusMessageHandler handler = mock(ClientStatusMessageHandler.class);
    private final ClientStatusMessageHandler.Factory handlerFactory = mock(ClientStatusMessageHandler.Factory.class);

    @Before
    public void before() throws Exception {
        when(handlerFactory.create(any())).thenReturn(handler);
    }

    @Test
    public void create_handlers_on_construction() throws Exception {
        createServer();

        verify(handlerFactory).create(any(Consumer.class));
    }

    @Test
    public void send_messages_to_handlers() throws Exception {
        final ClientStatusMessageImpl msg = ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, emptyList())
        );

        final UpdateServer server = createAndOpenServer();
        server.onMessage(encode(msg));

        verify(handler).onClientStatusMessage(msg);
    }

    @Test
    public void add_listener_on_open() {
        final UpdateServer server = createAndOpenServer();

        verify(geofenceStore).addListener(server);
        verify(alertProcessor).addListener(server);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final UpdateServer server = createAndOpenServer();

        server.onClose(session, mock(CloseReason.class));

        verify(geofenceStore).removeListener(server);
        verify(alertProcessor).removeListener(server);
        verify(handler).close();
    }

    @Test
    public void send_geofence_geometry_update() throws Exception {
        final List<Feature> features = mock(List.class);
        final FeatureCollection featureCollection = FeatureCollectionImpl.of(newArrayList(
                FeatureImpl.of(Optional.of("foo"), Optional.of(PointImpl.of(newArrayList(1.0, 2.0))), emptyMap())
        ));
        when(converter.toGeojson(any())).thenReturn(featureCollection);


        final UpdateServer server = createAndOpenServer();
        server.onGeometryChange(features);

        verify(converter).toGeojson(features);

        verifyMessage(GeofenceGeometryUpdateMessageImpl.of(featureCollection));
    }

    @Test
    public void send_geofence_violation_update_accounting_for_cumulative_changes() throws Exception {
        final EntityId geofenceIdA = EntityIdImpl.of("37");
        final EntityId geofenceIdB = EntityIdImpl.of("38");
        final Violation violationA = violation(geofenceIdA);
        final Violation violationB = violation(geofenceIdB);

        final UpdateServer server = createAndOpenServer();
        server.onViolationBegin(violationA);
        server.onViolationBegin(violationB);
        server.onViolationEnd(violationA);

        verifyMessage(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA)));
        verifyMessage(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdA, geofenceIdB)));
        verifyMessage(GeofenceViolationsUpdateMessageImpl.of(ImmutableList.of(geofenceIdB)));
    }

    @Test
    public void send_alert() throws Exception {
        final Alert alert = AlertImpl.of("foo", "bar");

        final UpdateServer server = createAndOpenServer();
        server.onAlert(alert);

        verifyMessage(AlertMessageImpl.of(alert));
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(encode(expected));
    }

    private Violation violation(EntityId geofenceId) {
        final Geofence geofence = mock(Geofence.class, RETURNS_DEEP_STUBS);
        when(geofence.feature().entityId()).thenReturn(geofenceId);
        return ViolationImpl.of(
                mock(Feature.class),
                geofence,
                "Hmmm"
        );
    }

    private UpdateServer createAndOpenServer() {
        final UpdateServer server = createServer();
        server.onOpen(session, mock(EndpointConfig.class));
        return server;
    }

    private UpdateServer createServer() {
        return new UpdateServer(
                geofenceStore,
                alertProcessor,
                newArrayList(handlerFactory),
                converter,
                OBJECT_MAPPER);
    }
}
