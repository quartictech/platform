package io.quartic.weyl.core;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerPopulator;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.model.SnapshotImpl;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;

public class LayerStoreShould {
    private static final LayerId LAYER_ID = LayerId.fromString("666");
    private static final LayerId OTHER_LAYER_ID = LayerId.fromString("777");
    private static final AttributeName ATTRIBUTE_NAME = AttributeNameImpl.of("timestamp");
    private static final Attributes ATTRIBUTES = () -> ImmutableMap.of(ATTRIBUTE_NAME, 1234);

    private final ObservableStore<EntityId, Feature> entityStore = mock(ObservableStore.class);
    private final LayerReducer layerReducer = mock(LayerReducer.class);
    private final PublishSubject<LayerPopulator> populators = PublishSubject.create();
    private final LayerStore store = LayerStoreImpl.builder()
            .populators(populators)
            .entityStore(entityStore)
            .layerReducer(layerReducer)
            .build();
    private final GeometryFactory factory = new GeometryFactory();

    @Test
    public void prevent_overwriting_an_existing_layer() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockLayerCreationFor(spec);

        createLayer(spec);
        createLayer(spec);

        verify(layerReducer, times(1)).create(any());
    }

    @Test
    public void resolve_layer_dependencies() throws Exception {
        final LayerSpec specDependency = spec(LAYER_ID);
        final LayerSpec specDependent = spec(OTHER_LAYER_ID);
        final Layer dependency = mockLayerCreationFor(specDependency);
        mockLayerCreationFor(specDependent);

        createLayer(specDependency);

        final LayerPopulator populator = mock(LayerPopulator.class);
        when(populator.dependencies()).thenReturn(singletonList(LAYER_ID)); // Specify another layer as a dependency
        when(populator.spec(any())).thenReturn(specDependent);
        when(populator.updates(any())).thenReturn(empty());
        populators.onNext(populator);

        verify(populator).spec(singletonList(dependency));
        verify(populator).updates(singletonList(dependency));
    }

    @Test
    public void apply_updates_to_layer_via_reducer() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockLayerReductionFor(mockLayerCreationFor(spec));

        createLayer(spec).onNext(updateFor(modelFeature("a")));

        verify(layerReducer).reduce(any(), eq(newArrayList(feature("a"))));
    }

    @Test
    public void put_updated_attributes_to_store() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockLayerReductionFor(mockLayerCreationFor(spec));

        createLayer(spec).onNext(updateFor(modelFeature("a"), modelFeature("b")));

        verify(entityStore).putAll(any(), eq(newArrayList(feature("a"), feature("b"))));
    }

    @Test
    public void emit_sequence_every_time_layer_is_created() throws Exception {
        final LayerSpec specA = spec(LAYER_ID);
        final LayerSpec specB = spec(OTHER_LAYER_ID);
        mockLayerCreationFor(specA);
        mockLayerCreationFor(specB);

        TestSubscriber<LayerSnapshotSequence> sub = TestSubscriber.create();
        store.snapshotSequences().subscribe(sub);

        createLayer(specA);
        createLayer(specB);

        assertThat(transform(sub.getOnNextEvents(), LayerSnapshotSequence::id), contains(LAYER_ID, OTHER_LAYER_ID));
    }

    @Test
    public void emit_snapshot_every_time_layer_is_updated() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        final Layer original = mockLayerCreationFor(spec);
        final Layer update1 = mockLayerReductionFor(original);
        final Layer update2 = mockLayerReductionFor(update1);

        TestSubscriber<LayerSnapshotSequence.Snapshot> sub = TestSubscriber.create();
        store.snapshotSequences().subscribe(s -> s.snapshots().subscribe(sub)); // Subscribe to the nested snapshot observable

        PublishSubject<LayerUpdate> updates = createLayer(spec);
        updates.onNext(updateFor(modelFeature("a")));
        updates.onNext(updateFor(modelFeature("b")));

        assertThat(sub.getOnNextEvents(), contains(
                SnapshotImpl.of(original, emptyList()),
                SnapshotImpl.of(update1, newArrayList(feature("a"))),
                SnapshotImpl.of(update2, newArrayList(feature("b")))
        ));
    }

    @Test
    public void notify_subscribers_of_current_layer_state_and_subsequent_updates_upon_subscribing() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        final Layer original = mockLayerCreationFor(spec);
        final Layer firstUpdate = mockLayerReductionFor(original);
        final Layer secondUpdate = mockLayerReductionFor(firstUpdate);

        PublishSubject<LayerUpdate> updates = createLayer(spec);
        updates.onNext(updateFor());   // Observed before subscription

        TestSubscriber<Layer> subscriber = TestSubscriber.create();
        store.layer(LAYER_ID).subscribe(subscriber);

        updates.onNext(updateFor());   // Observed after subscription

        assertThat(subscriber.getOnNextEvents(), contains(firstUpdate, secondUpdate));
    }

    private Layer mockLayerCreationFor(LayerSpec spec) {
        final Layer layer = mock(Layer.class);
        when(layer.spec()).thenReturn(spec);
        when(layerReducer.create(spec)).thenReturn(layer);
        return layer;
    }

    private Layer mockLayerReductionFor(Layer layer) {
        final LayerSpec originalSpec = layer.spec();
        final Layer updatedLayer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(updatedLayer.spec()).thenReturn(originalSpec);
        when(layerReducer.reduce(eq(layer), any())).thenReturn(updatedLayer);
        return updatedLayer;
    }

    private LayerUpdate updateFor(NakedFeature... features) {
        return LayerUpdateImpl.of(asList(features));
    }

    private NakedFeature modelFeature(String externalId) {
        return NakedFeatureImpl.of(
                Optional.ofNullable(externalId),
                factory.createPoint(new Coordinate(123.0, 456.0)),
                ATTRIBUTES
        );
    }

    private Feature feature(String externalId) {
        return FeatureImpl.of(
                EntityIdImpl.of(LAYER_ID.uid() + "/" + externalId),
                factory.createPoint(new Coordinate(123.0, 456.0)),
                ATTRIBUTES
        );
    }

    private PublishSubject<LayerUpdate> createLayer(LayerSpec spec) {
        final PublishSubject<LayerUpdate> updates = PublishSubject.create();
        populators.onNext(LayerPopulator.withoutDependencies(spec, updates));
        return updates;
    }

    private LayerSpec spec(LayerId id) {
        final LayerSpec spec = mock(LayerSpec.class, RETURNS_DEEP_STUBS);
        when(spec.id()).thenReturn(id);
        when(spec.metadata().name()).thenReturn("foo");
        return spec;
    }
}
