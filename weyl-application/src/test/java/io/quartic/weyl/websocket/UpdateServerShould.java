package io.quartic.weyl.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.FeatureCollectionImpl;
import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertImpl;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceStore;
import io.quartic.weyl.core.geofence.Violation;
import io.quartic.weyl.core.geofence.ViolationImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.websocket.message.*;
import io.quartic.weyl.update.SelectionDrivenUpdateGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.common.serdes.ObjectMappers.encode;
import static io.quartic.weyl.core.geojson.Utils.fromJts;
import static io.quartic.weyl.core.model.Attributes.EMPTY_ATTRIBUTES;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatortoWgs84;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static rx.Observable.empty;
import static rx.Observable.just;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final GeometryTransformer transformer = webMercatortoWgs84();
    private final LayerStore layerStore = mock(LayerStore.class);
    private final GeofenceStore geofenceStore = mock(GeofenceStore.class);
    private final AlertProcessor alertProcessor = mock(AlertProcessor.class);
    private final Multiplexer<Integer, EntityId, Feature> mux = mock(Multiplexer.class);
    private final SelectionDrivenUpdateGenerator generator = mock(SelectionDrivenUpdateGenerator.class);

    @Before
    public void setUp() throws Exception {
        when(mux.call(any())).thenReturn(empty());
    }

    @Test
    public void add_listener_on_open() {
        final UpdateServer server = createAndOpenServer();

        verify(geofenceStore).addListener(server);
        verify(alertProcessor).addListener(server);
    }

    @Test
    public void remove_listener_on_close() {
        final UpdateServer server = createAndOpenServer();

        server.onClose(session, mock(CloseReason.class));

        verify(geofenceStore).removeListener(server);
        verify(alertProcessor).removeListener(server);
    }

    @Test
    public void send_geofence_geometry_update() throws Exception {
        final Geometry geometry = new GeometryFactory().createPoint(new Coordinate(1.0, 2.0));
        final Feature feature = FeatureImpl.of(
                EntityIdImpl.of("xyz/123"),
                geometry,
                EMPTY_ATTRIBUTES
        );

        final UpdateServer server = createAndOpenServer();
        server.onGeometryChange(ImmutableList.of(feature));

        verifyMessage(GeofenceGeometryUpdateMessageImpl.of(FeatureCollectionImpl.of(ImmutableList.of(
                io.quartic.geojson.FeatureImpl.of(
                        Optional.empty(),
                        Optional.of(fromJts(transformer.transform(geometry))),
                        ImmutableMap.of("_entityId", "xyz/123")
                )
        ))));
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

    @Test
    public void route_entity_list_to_mux() throws Exception {
        final ArrayList<EntityId> ids = newArrayList(EntityIdImpl.of("123"));
        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = TestSubscriber.create();
        when(mux.call(any())).then(invocation -> {
            final Observable<Pair<Integer, List<EntityId>>> observable = invocation.getArgument(0);
            observable.subscribe(subscriber);
            return empty();
        });

        final UpdateServer server = createAndOpenServer();
        final String encode = encode(ClientStatusMessageImpl.of(emptyList(), SelectionStatusImpl.of(42, ids)));
        System.out.println(encode);
        server.onMessage(encode);
        subscriber.awaitValueCount(1, 100, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents().get(0), equalTo(Pair.of(42, ids)));
    }

    @Test
    public void process_entity_updates_and_send_results() throws Exception {
        final ArrayList<EntityId> ids = newArrayList(EntityIdImpl.of("123"));
        final Object data = mock(Object.class);
        final List<Feature> features = newArrayList(mock(Feature.class), mock(Feature.class));
        when(mux.call(any())).thenReturn(just(Pair.of(56, features)));
        when(generator.name()).thenReturn("foo");
        when(generator.generate(any())).thenReturn(data);

        final UpdateServer server = createAndOpenServer();
        server.onMessage(encode(ClientStatusMessageImpl.of(emptyList(), SelectionStatusImpl.of(42, ids))));

        verify(generator).generate(features);
        verifyMessage(SelectionDrivenUpdateMessageImpl.of("foo", 56, data));
    }

    @Test
    public void unsubscribe_from_mux_on_close() throws Exception {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        final Observable<Pair<Integer, List<Feature>>> observable = empty();
        when(mux.call(any())).thenReturn(observable.doOnUnsubscribe(() -> unsubscribed.set(true)));

        createAndOpenServer().onClose(session, mock(CloseReason.class));

        assertThat(unsubscribed.get(), equalTo(true));
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
        final UpdateServer server = new UpdateServer(
                layerStore,
                mux,
                newArrayList(generator),
                geofenceStore,
                alertProcessor,
                transformer,
                OBJECT_MAPPER);
        server.onOpen(session, mock(EndpointConfig.class));
        return server;
    }
}
