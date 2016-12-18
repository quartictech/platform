package io.quartic.weyl.websocket;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.geofence.Geofence;
import io.quartic.weyl.core.geofence.GeofenceImpl;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.geofence.GeofenceViolationDetector;
import io.quartic.weyl.core.geofence.ViolationBeginEventImpl;
import io.quartic.weyl.core.geofence.ViolationEndEventImpl;
import io.quartic.weyl.core.geofence.ViolationEvent;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequenceImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.model.SnapshotImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceGeometryUpdateMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessage;
import io.quartic.weyl.websocket.message.GeofenceViolationsUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.vividsolutions.jts.operation.buffer.BufferOp.bufferOp;
import static io.quartic.weyl.core.alert.Alert.Level.SEVERE;
import static io.quartic.weyl.core.alert.Alert.Level.WARNING;
import static io.quartic.weyl.core.alert.AlertProcessor.ALERT_LEVEL;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.from;
import static rx.Observable.just;

public class GeofenceStatusHandlerShould {
    private final Attributes featureAttributes = mock(Attributes.class);
    private final NakedFeature featureA = NakedFeatureImpl.of(Optional.empty(), polygon(5.0), featureAttributes);
    private final NakedFeature featureB = NakedFeatureImpl.of(Optional.empty(), polygon(6.0), featureAttributes);
    private final FeatureCollection featureCollection = mock(FeatureCollection.class);

    private final GeofenceViolationDetector detector = mock(GeofenceViolationDetector.class);

    private final Interceptor<LayerSnapshotSequence> interceptor = Interceptor.create();
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final FeatureConverter converter = mock(FeatureConverter.class);
    private final ClientStatusMessageHandler handler = new GeofenceStatusHandler(
            detector,
            snapshotSequences.compose(interceptor),
            converter
    );

    @Before
    public void before() throws Exception {
        when(converter.toModel(any())).thenReturn(newArrayList(featureA, featureB));
        when(converter.toGeojson(any())).thenReturn(featureCollection);
    }

    // TODO: individual tests for clear/begin/end events

    @Test
    public void send_violation_updates_accounting_for_cumulative_changes() throws Exception {
        final EntityId entityIdA = mock(EntityId.class);
        final EntityId entityIdB = mock(EntityId.class);
        final EntityId geofenceIdA = mock(EntityId.class);
        final EntityId geofenceIdB = mock(EntityId.class);
        mockDetectorBehaviour(just(
                ViolationBeginEventImpl.of(entityIdA, geofenceIdA, SEVERE),
                ViolationBeginEventImpl.of(entityIdB, geofenceIdB, WARNING),
                ViolationEndEventImpl.of(entityIdA, geofenceIdA, SEVERE)
        ));

        TestSubscriber<SocketMessage> sub = subscribeToHandler(status(identity()));

        assertThat(filter(sub.getOnNextEvents(), msg -> msg instanceof GeofenceViolationsUpdateMessage), contains(
                GeofenceViolationsUpdateMessageImpl.of(newArrayList(geofenceIdA), 0, 0, 1),
                GeofenceViolationsUpdateMessageImpl.of(newArrayList(geofenceIdA, geofenceIdB), 0, 1, 1),
                GeofenceViolationsUpdateMessageImpl.of(newArrayList(geofenceIdB), 0, 1, 0)
        ));
    }

    @Test
    public void set_geofence_based_on_features() throws Exception {
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(status(builder -> builder.features(featureCollection)));

        verify(converter, atLeastOnce()).toModel(featureCollection);
        assertGeofences(interceptor, "custom", featureA, featureB);
    }

    @Test
    public void set_geofence_based_on_layer() throws Exception {
        final LayerId layerId = mock(LayerId.class);
        final LayerSpec spec = mock(LayerSpec.class);
        final Layer layer = createLayer(layerId, spec);
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();
        snapshotSequences.onNext(LayerSnapshotSequenceImpl.of(spec, just(SnapshotImpl.of(layer, emptyList()))));

        subscribeToHandler(status(builder -> builder.layerId(layerId)));

        assertGeofences(interceptor, "xyz", featureA, featureB);
    }

    private Layer createLayer(LayerId layerId, LayerSpec spec) {
        final io.quartic.weyl.core.feature.FeatureCollection featureCollection = mock(io.quartic.weyl.core.feature.FeatureCollection.class);
        final Layer layer = mock(Layer.class);
        when(spec.id()).thenReturn(layerId);
        when(layer.features()).thenReturn(featureCollection);
        when(featureCollection.stream()).thenAnswer(invocation -> newArrayList(modelFeatureOf(featureA), modelFeatureOf(featureB)).stream());
        return layer;
    }

    @Test
    public void send_geometry_update_when_geofence_changes() throws Exception {
        mockDetectorBehaviour();

        final TestSubscriber<SocketMessage> sub = subscribeToHandler(status(builder -> builder.features(featureCollection)));

        assertThat(filter(sub.getOnNextEvents(), msg -> msg instanceof GeofenceGeometryUpdateMessage),
                contains(GeofenceGeometryUpdateMessageImpl.of(featureCollection)));
    }

    @Test
    public void set_empty_geofence_when_disabled() throws Exception {
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(status(builder -> builder.enabled(false)));

        assertGeofences(interceptor, "");
    }

    @Test
    public void set_level_attribute_based_on_attribute_from_features() throws Exception {
        when(featureAttributes.attributes()).thenReturn(singletonMap(ALERT_LEVEL, "warning"));
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(status(builder -> builder.features(mock(FeatureCollection.class))));

        assertGeofences(interceptor, "custom", WARNING, featureA, featureB);
    }

    @Test
    public void ignore_non_polygons() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                featureA,
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(status(builder -> builder.features(features)));

        assertGeofences(interceptor, "custom", featureA);
    }

    @Test
    public void add_buffering_to_geometries() throws Exception {
        final FeatureCollection features = mock(FeatureCollection.class);
        when(converter.toModel(any())).thenReturn(newArrayList(
                NakedFeatureImpl.of(Optional.empty(), point(), featureAttributes)
        ));
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(status(builder -> builder.features(features).bufferDistance(1.0)));

        assertGeofences(interceptor, "custom", NakedFeatureImpl.of(Optional.empty(), bufferOp(point(), 1.0), featureAttributes));
    }

    @Test
    public void ignore_status_changes_not_involving_geofence_change() throws Exception {
        final ClientStatusMessage statusA = status(identity());
        final ClientStatusMessage statusB = status(identity());
        when(statusA.openLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));
        when(statusB.openLayerIds()).thenReturn(newArrayList(mock(LayerId.class)));
        final Interceptor<Collection<Geofence>> interceptor = mockDetectorBehaviour();

        subscribeToHandler(statusA, statusB);

        assertThat(interceptor.values(), hasSize(1));
    }

    // This covers the non-reactive hackery we still have in the implementation
    @Test
    public void unsubscribe_from_snapshots_on_unsubscribe() throws Exception {
        mockDetectorBehaviour();

        subscribeToHandler(status(identity())).unsubscribe();

        assertThat(interceptor.unsubscribed(), equalTo(true));
    }

    private ClientStatusMessage status(Function<GeofenceStatusImpl.Builder, GeofenceStatusImpl.Builder> builderHacks) {
        GeofenceStatusImpl.Builder builder = GeofenceStatusImpl.builder()
                .enabled(true)
                .type(GeofenceType.INCLUDE)
                .features(Optional.empty())
                .defaultLevel(SEVERE)
                .bufferDistance(0.0);
        builder = builderHacks.apply(builder);
        ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.geofence()).thenReturn(builder.build());
        return msg;
    }

    private TestSubscriber<SocketMessage> subscribeToHandler(ClientStatusMessage... statuses) {
        TestSubscriber<SocketMessage> sub = TestSubscriber.create();
        from(statuses)
//                .concatWith(never())    // Because we're expecting an infinite stream
                .compose(handler)
                .subscribe(sub);
        return sub;
    }

    public class IoContext {
        private final PublishSubject<ClientStatusMessage> input = PublishSubject.create();
        private final TestSubscriber<SocketMessage> output = TestSubscriber.create();

        public IoContext() {
            input.compose(handler).subscribe(output);
        }
    }



    private Interceptor<Collection<Geofence>> mockDetectorBehaviour() {
        return mockDetectorBehaviour(empty());
    }

    private Interceptor<Collection<Geofence>> mockDetectorBehaviour(Observable<ViolationEvent> violationEvents) {
        final Interceptor<Collection<Geofence>> interceptor = Interceptor.create();
        when(detector.call(any())).thenAnswer(invocation -> {
            Observable<Collection<Geofence>> obs = invocation.getArgument(0);
            return obs
                    .compose(interceptor)
                    .concatMap(x -> violationEvents);//.concatWith(never())); // Simulate infinite stream
        });
        return interceptor;
    }

    private void assertGeofences(Interceptor<Collection<Geofence>> interceptor, String id, NakedFeature... features) {
        assertGeofences(interceptor, id, SEVERE, features);
    }

    private void assertGeofences(Interceptor<Collection<Geofence>> interceptor, String id, Alert.Level level, NakedFeature... features) {
        assertThat(interceptor.values(), contains(
                stream(features)
                        .map(p -> geofenceOf(id, level, p.geometry()))
                        .collect(toList()))
        );
    }

    private Geofence geofenceOf(String id, Alert.Level level, Geometry geometry) {
        return GeofenceImpl.of(
                GeofenceType.INCLUDE,
                io.quartic.weyl.core.model.FeatureImpl.of(
                        EntityIdImpl.of("geofence/" + id),
                        geometry,
                        AttributesImpl.of(singletonMap(ALERT_LEVEL, level))
                )
        );
    }

    private Feature modelFeatureOf(NakedFeature feature) {
        return io.quartic.weyl.core.model.FeatureImpl.of(
                EntityIdImpl.of("xyz"),
                feature.geometry(),
                feature.attributes());
    }

    private Geometry point() {
        return new GeometryFactory().createPoint(new Coordinate(1.0, 2.0));
    }

    private Geometry polygon(double offset) {
        return new GeometryFactory().createPolygon(new Coordinate[] {
                new Coordinate(1.0 + offset, 2.0 + offset),
                new Coordinate(1.0 + offset, 3.0 + offset),
                new Coordinate(2.0 + offset, 3.0 + offset),
                new Coordinate(2.0 + offset, 2.0 + offset),
                new Coordinate(1.0 + offset, 2.0 + offset)
        });
    }
}
