package io.quartic.weyl.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.geofence.ImmutableLiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChange;
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
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.LayerUpdateImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
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
    public void list_created_layers() throws Exception {
        final LayerSpec spec1 = spec(LAYER_ID);
        final LayerSpec spec2 = spec(OTHER_LAYER_ID);
        final List<Layer> expectedLayers = newArrayList(mockLayerCreationFor(spec1), mockLayerCreationFor(spec2));

        createLayer(spec1);
        createLayer(spec2);
        final List<Layer> layers = store.listLayers();

        assertThat(layers, equalTo(expectedLayers));
    }

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
    public void apply_updates_to_layer() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        final Layer updatedLayer = mockLayerReductionFor(mockLayerCreationFor(spec));

        createLayer(spec).onNext(updateFor(modelFeature("a")));

        verify(layerReducer).reduce(any(), eq(newArrayList(feature("a"))));
        assertThat(store.getLayer(LAYER_ID).get(), equalTo(updatedLayer));
    }

    @Test
    public void put_updated_attributes_to_store() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockLayerReductionFor(mockLayerCreationFor(spec));

        createLayer(spec).onNext(updateFor(modelFeature("a"), modelFeature("b")));

        verify(entityStore).putAll(any(), eq(newArrayList(feature("a"), feature("b"))));
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
        store.layersForLayerId(LAYER_ID).subscribe(subscriber);

        updates.onNext(updateFor());   // Observed after subscription

        assertThat(subscriber.getOnNextEvents(), contains(firstUpdate, secondUpdate));
    }

    @Test
    public void notify_observers_on_new_features() throws Exception {
        final LayerSpec spec = spec(LAYER_ID);
        mockLayerReductionFor(mockLayerCreationFor(spec));

        PublishSubject<LayerUpdate> updates = createLayer(spec);

        TestSubscriber<LiveLayerChange> sub = TestSubscriber.create();
        store.liveLayerChanges(LAYER_ID).subscribe(sub);

        updates.onNext(updateFor(modelFeature("a")));

        Collection<Feature> features = ImmutableList.of(feature("a"));
        sub.assertReceivedOnNext(newArrayList(liveLayerChange(LAYER_ID, features)));
    }

    @Test
    public void notify_on_layer_creation() {
        final LayerSpec specA = spec(LAYER_ID);
        final LayerSpec specB = spec(OTHER_LAYER_ID);
        final Layer layerA = mockLayerCreationFor(specA);
        final Layer layerB = mockLayerCreationFor(specB);

        createLayer(specA);
        TestSubscriber<Collection<Layer>> sub = TestSubscriber.create();
        store.allLayers().subscribe(sub);
        assertThat(sub.getOnNextEvents().get(0), contains(layerA));

        createLayer(specB);
        assertThat(sub.getOnNextEvents().get(1), contains(layerA, layerB));
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

    private LiveLayerChange liveLayerChange(LayerId layerId, Collection<Feature> features) {
        return ImmutableLiveLayerChange.of(layerId, features);
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
