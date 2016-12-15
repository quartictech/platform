package io.quartic.weyl.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.ComputationResults;
import io.quartic.weyl.core.compute.ComputationResultsImpl;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.geofence.ImmutableLiveLayerChange;
import io.quartic.weyl.core.geofence.LiveLayerChange;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceDescriptor;
import io.quartic.weyl.core.source.SourceDescriptorImpl;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.hamcrest.Matchers;
import org.junit.Test;
import rx.Observable;
import rx.exceptions.OnErrorNotImplementedException;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static rx.Observable.empty;
import static rx.Observable.just;

public class LayerStoreShould {
    private static final LayerId LAYER_ID = LayerId.fromString("666");
    private static final LayerId OTHER_LAYER_ID = LayerId.fromString("777");
    private static final AttributeName ATTRIBUTE_NAME = AttributeNameImpl.of("timestamp");
    private static final Attributes ATTRIBUTES = () -> ImmutableMap.of(ATTRIBUTE_NAME, 1234);

    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerIdImpl::of);
    private final ObservableStore<EntityId, Feature> entityStore = mock(ObservableStore.class);
    private final LayerComputation.Factory computationFactory = mock(LayerComputation.Factory.class);
    private final PublishSubject<SourceDescriptor> sources = PublishSubject.create();
    private final LayerStore store = LayerStoreImpl.builder()
            .sources(sources)
            .entityStore(entityStore)
            .lidGenerator(lidGenerator)
            .computationFactory(computationFactory)
            .build();
    private final GeometryFactory factory = new GeometryFactory();

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = metadata("foo", "bar");
        final LayerMetadata lm2 = metadata("cheese", "monkey");

        final AttributeSchema as1 = schema("foo");
        final AttributeSchema as2 = schema("bar");

        createLayer(SourceDescriptorImpl.of(LAYER_ID, lm1, IDENTITY_VIEW, as1, true, empty()));
        createLayer(SourceDescriptorImpl.of(OTHER_LAYER_ID, lm2, IDENTITY_VIEW, as2, true, empty()));

        final Collection<Layer> layers = store.listLayers();

        assertThat(map(layers, Layer::layerId), containsInAnyOrder(LAYER_ID, OTHER_LAYER_ID));
        assertThat(map(layers, Layer::metadata), containsInAnyOrder(lm1, lm2));
        assertThat(map(layers, Layer::indexable), containsInAnyOrder(true, true));
        assertThat(map(layers, Layer::schema), containsInAnyOrder(as1, as2));
    }

    private <T, R> List<R> map(Collection<T> input, Function<T, R> mapper) {
        return input.stream().map(mapper).collect(toList());
    }

    @Test
    public void preserve_core_schema_info_upon_update() throws Exception {
        createLayer(LAYER_ID, just(updateFor(modelFeature("a"))));

        final Layer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.schema().blessedAttributes(), Matchers.contains(AttributeNameImpl.of("blah")));
    }

    @Test
    public void add_observed_features_to_layer() throws Exception {
        createLayer(LAYER_ID, just(
                updateFor(modelFeature("a")),
                updateFor(modelFeature("b"))
        ));

        final Layer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.features(), containsInAnyOrder(feature("a"), feature("b")));
    }

    @Test
    public void put_attributes_to_store() throws Exception {
        createLayer(LAYER_ID, just(updateFor(modelFeature("a"), modelFeature("b"))));

        verify(entityStore).putAll(any(), eq(newArrayList(feature("a"), feature("b"))));
    }

    @Test
    public void notify_subscribers_of_all_features_upon_subscribing() throws Exception {
        PublishSubject<SourceUpdate> updates = PublishSubject.create();
        createLayer(LAYER_ID, updates);

        updates.onNext(updateFor(modelFeature("a")));   // Observed before

        TestSubscriber<Layer> subscriber = TestSubscriber.create();
        store.layersForLayerId(LAYER_ID).subscribe(subscriber);

        updates.onNext(updateFor(modelFeature("b")));   // Observed after

        assertThat(subscriber.getOnNextEvents().size(), equalTo(2));
        assertThat(subscriber.getOnNextEvents().get(1).features(), containsInAnyOrder(feature("a"), feature("b")));
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void throw_if_create_called_on_an_existing_layer() throws Exception {
        createLayer(LAYER_ID, empty());
        createLayer(LAYER_ID, empty());
    }

    @Test
    public void notify_observers_on_new_features() throws Exception {
        PublishSubject<SourceUpdate> updates = PublishSubject.create();
        createLayer(LAYER_ID, updates);

        Observable<LiveLayerChange> newFeatures = store.liveLayerChanges(LAYER_ID);
        TestSubscriber<LiveLayerChange> sub = TestSubscriber.create();
        newFeatures.subscribe(sub);

        updates.onNext(updateFor(modelFeature("a")));

        Collection<Feature> features = ImmutableList.of(feature("a"));
        sub.assertReceivedOnNext(newArrayList(liveLayerChange(LAYER_ID, features)));
    }

    @Test
    public void notify_on_updates_to_layers() throws Exception {
        PublishSubject<SourceUpdate> updates = PublishSubject.create();
        createLayer(LAYER_ID, updates);

        TestSubscriber<Layer> sub = TestSubscriber.create();
        store.layersForLayerId(LAYER_ID).subscribe(sub);

        updates.onNext(updateFor(modelFeature("a")));

        List<Layer> layers = sub.getOnNextEvents();
        // This actually gets emitted twice currently (once empty, then once with the feature)
        assertThat(layers.size(), equalTo(2));
        assertThat(layers.get(0).features().size(), equalTo(0));
        // Second update contains feature "a"
        assertThat(layers.get(1).features().size(), equalTo(1));
        assertThat(layers.get(1).features(), containsInAnyOrder(feature("a")));

        layers = sub.getOnNextEvents();
        updates.onNext(updateFor(modelFeature("b")));

        assertThat(layers.size(), equalTo(3));
        assertThat(layers.get(2).features(), containsInAnyOrder(feature("a"), feature("b")));

        assertThat(layers.get(1).schema(),
                equalTo(AttributeSchemaImpl.copyOf(schema("blah"))
                        .withAttributes(ImmutableMap.of(ATTRIBUTE_NAME, AttributeImpl.of(NUMERIC, Optional.of(ImmutableSet.of(1234)))))));
    }

    @Test
    public void notify_on_layer_addition() {
        createLayer(LAYER_ID, empty());

        TestSubscriber<Collection<Layer>> sub = TestSubscriber.create();
        store.allLayers().subscribe(sub);

        List<Collection<Layer>> layerEvents = sub.getOnNextEvents();
        assertThat(layerEvents.size(), equalTo(1));
        assertThat(layerEvents.get(0).size(), equalTo(1));
        assertThat(layerEvents.get(0).stream().map(Layer::layerId).collect(toList()), containsInAnyOrder(LAYER_ID));

        createLayer(OTHER_LAYER_ID, empty());
        layerEvents = sub.getOnNextEvents();
        assertThat(layerEvents.size(), equalTo(2));
        assertThat(layerEvents.get(1).size(), equalTo(2));
        assertThat(layerEvents.get(1).stream().map(Layer::layerId).collect(toList()), containsInAnyOrder(LAYER_ID, OTHER_LAYER_ID));
    }

    @Test
    public void create_layer_for_computed_results() throws Exception {
        final LayerMetadata metadata = mock(LayerMetadata.class);
        final AttributeSchema schema = schema("pets");
        final ComputationResults results = ComputationResultsImpl.of(
                metadata,
                schema,
                newArrayList(modelFeature("a"), modelFeature("b"))
        );
        final ComputationSpec spec = mock(ComputationSpec.class);
        when(computationFactory.compute(any(), any())).thenReturn(Optional.of(results));

        final LayerId layerId = store.compute(spec).get();
        final Layer layer = store.getLayer(layerId).get();

        verify(computationFactory).compute(store, spec);
        assertThat(layer.metadata(), equalTo(metadata));
        assertThat(layer.schema(), equalTo(AttributeSchemaImpl.copyOf(schema)
                .withAttributes(ImmutableMap.of(ATTRIBUTE_NAME, AttributeImpl.of(NUMERIC, Optional.of(newHashSet(1234)))))
        ));
        assertThat(layer.features(), containsInAnyOrder(feature("1", "a"), feature("1", "b")));
    }

    @Test
    public void calculate_indices_for_indexable_layer() throws Exception {
        assertThatLayerIndexedFeaturesHasSize(true, 1);
    }

    @Test
    public void not_calculate_indices_for_non_indexable_layer() throws Exception {
        assertThatLayerIndexedFeaturesHasSize(false, 0);
    }

    private void assertThatLayerIndexedFeaturesHasSize(boolean indexable, int size) {
        createLayer(LAYER_ID, indexable, just(updateFor(modelFeature("a"))));

        final Layer layer = store.getLayer(LAYER_ID).get();

        assertThat(layer.indexedFeatures(), hasSize(size));
    }

    private SourceUpdate updateFor(NakedFeature... features) {
        return SourceUpdateImpl.of(asList(features));
    }

    private NakedFeature modelFeature(String externalId) {
        return NakedFeatureImpl.of(
                Optional.ofNullable(externalId),
                factory.createPoint(new Coordinate(123.0, 456.0)),
                ATTRIBUTES
        );
    }

    private Feature feature(String externalId) {
        return feature(LAYER_ID.uid(), externalId);
    }

    private Feature feature(String layerId, String externalId) {
        return FeatureImpl.of(
                EntityIdImpl.of(layerId + "/" + externalId),
                factory.createPoint(new Coordinate(123.0, 456.0)),
                ATTRIBUTES
        );
    }

    private LiveLayerChange liveLayerChange(LayerId layerId, Collection<Feature> features) {
        return ImmutableLiveLayerChange.of(layerId, features);
    }

    private void createLayer(LayerId layerId, Observable<SourceUpdate> updates) {
        createLayer(layerId, false, updates);
    }

    private void createLayer(LayerId layerId, boolean indexable, Observable<SourceUpdate> updates) {
        final SourceDescriptor descriptor = SourceDescriptorImpl.of(layerId, metadata("foo", "bar"), IDENTITY_VIEW, schema("blah"), indexable, updates);
        createLayer(descriptor);
    }

    private void createLayer(SourceDescriptor descriptor) {
        sources.onNext(descriptor);
    }

    private AttributeSchema schema(String blessed) {
        return AttributeSchemaImpl.builder().blessedAttribute(AttributeNameImpl.of(blessed)).build();
    }

    private LayerMetadata metadata(String name, String description) {
        return LayerMetadataImpl.of(name, description, Optional.empty(), Optional.empty());
    }
}
