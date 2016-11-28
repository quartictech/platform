package io.quartic.weyl.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.uid.SequenceUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.compute.ComputationResults;
import io.quartic.weyl.core.compute.ComputationResultsImpl;
import io.quartic.weyl.core.compute.ComputationSpec;
import io.quartic.weyl.core.compute.LayerComputation;
import io.quartic.weyl.core.live.LayerState;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.google.common.collect.Iterables.getOnlyElement;
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

public class LayerStoreShould {
    private static final LayerId LAYER_ID = LayerIdImpl.of("666");
    private static final LayerId OTHER_LAYER_ID = LayerIdImpl.of("777");
    private static final AttributeName ATTRIBUTE_NAME = AttributeNameImpl.of("timestamp");
    private static final Attributes ATTRIBUTES = () -> ImmutableMap.of(ATTRIBUTE_NAME, 1234);

    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerIdImpl::of);
    private final ObservableStore<EntityId, Feature> entityStore = mock(ObservableStore.class);
    private final LayerComputation.Factory computationFactory = mock(LayerComputation.Factory.class);
    private final LayerStore store = LayerStoreImpl.builder()
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

        store.createLayer(LAYER_ID, lm1, IDENTITY_VIEW, as1, true);
        store.createLayer(OTHER_LAYER_ID, lm2, IDENTITY_VIEW, as2, true);

        final Collection<Layer> layers = store.listLayers();

        assertThat(layers.stream().map(Layer::layerId).collect(toList()),
                containsInAnyOrder(LAYER_ID, OTHER_LAYER_ID));
        assertThat(layers.stream().map(Layer::metadata).collect(toList()),
                containsInAnyOrder(lm1, lm2));
        assertThat(layers.stream().map(Layer::indexable).collect(toList()),
                containsInAnyOrder(true, true));
        assertThat(layers.stream().map(Layer::schema).collect(toList()),
                containsInAnyOrder(as1, as2));
    }

    @Test
    public void not_list_layer_once_deleted() throws Exception {
        createLayer(LAYER_ID);
        store.deleteLayer(LAYER_ID);

        assertThat(store.listLayers(), empty());
    }

    @Test
    public void preserve_core_schema_info_upon_update() throws Exception {
        final Action1<SourceUpdate> action = createLayer(LAYER_ID);

        Observable.just(updateFor(modelFeature("a"))).subscribe(action);

        final Layer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.schema().blessedAttributes(), Matchers.contains(AttributeNameImpl.of("blah")));
    }

    @Test
    public void add_observed_features_to_layer() throws Exception {
        final Action1<SourceUpdate> action = createLayer(LAYER_ID);

        Observable.just(
                updateFor(modelFeature("a")),
                updateFor(modelFeature("b"))
        ).subscribe(action);

        final Layer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.features(),
                containsInAnyOrder(feature("a"), feature("b")));
    }

    @Test
    public void put_attributes_to_store() throws Exception {
        final Action1<SourceUpdate> action = createLayer(LAYER_ID);

        Observable.just(updateFor(modelFeature("a"), modelFeature("b"))).subscribe(action);


        verify(entityStore).putAll(eq(newArrayList(feature("a"), feature("b"))), any());
    }


    // TODO: using subjects is kind of gross (see e.g. http://tomstechnicalblog.blogspot.co.uk/2016/03/rxjava-problem-with-subjects.html)
    // Luckily, this should go away once we model downstream stuff reactively too
    @Test
    public void notify_subscribers_of_all_features_upon_subscribing() throws Exception {
        final Action1<SourceUpdate> sub = createLayer(LAYER_ID);

        PublishSubject<SourceUpdate> subject = PublishSubject.create();
        subject.subscribe(sub);

        subject.onNext(updateFor(modelFeature("a")));   // Observed before

        TestSubscriber<Layer> subscriber = TestSubscriber.create();
        store.layersForLayerId(LAYER_ID).subscribe(subscriber);

        subject.onNext(updateFor(modelFeature("b")));   // Observed after

        assertThat(subscriber.getOnNextEvents().size(), equalTo(2));
        assertThat(subscriber.getOnNextEvents().get(1).features(),
                containsInAnyOrder(
                        feature("a"),
                        feature("b")
                ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_create_called_on_an_existing_layer() throws Exception {
        createLayer(LAYER_ID);
        createLayer(LAYER_ID);
    }

    @Test
    public void notify_observers_on_new_features() throws Exception {
        final Action1<SourceUpdate> action = createLayer(LAYER_ID);

        Observable<Collection<Feature>> newFeatures = store.newFeatures(LAYER_ID);

        TestSubscriber<Collection<Feature>> sub = TestSubscriber.create();
        newFeatures.subscribe(sub);

        Observable.just(
                updateFor(modelFeature("a"))
        ).subscribe(action);

        Collection<Feature> features = ImmutableList.of(feature("a"));
        sub.assertReceivedOnNext(ImmutableList.of(features));
    }

    @Test
    public void notify_on_updates_to_layers() throws Exception {
        final Action1<SourceUpdate> action = createLayer(LAYER_ID);

        TestSubscriber<Layer> sub = TestSubscriber.create();
        store.layersForLayerId(LAYER_ID).subscribe(sub);

        PublishSubject<SourceUpdate> sourceUpdates = PublishSubject.create();
        sourceUpdates.subscribe(action);

        sourceUpdates.onNext(updateFor(modelFeature("a")));

        List<Layer> layers = sub.getOnNextEvents();
        // This actually gets emitted twice currently (once empty, then once with the feature)
        assertThat(layers.size(), equalTo(2));
        assertThat(layers.get(0).features().size(), equalTo(0));
        // Second update contains feature "a"
        assertThat(layers.get(1).features().size(), equalTo(1));
        assertThat(layers.get(1).features(), containsInAnyOrder(feature("a")));

        layers = sub.getOnNextEvents();
        sourceUpdates.onNext(updateFor(modelFeature("b")));
        assertThat(layers.size(), equalTo(3));
        assertThat(layers.get(2).features(), containsInAnyOrder(feature("a"), feature("b")));

        assertThat(layers.get(1).schema(),
                equalTo(AttributeSchemaImpl.copyOf(schema("blah"))
                        .withAttributes(ImmutableMap.of(ATTRIBUTE_NAME, AttributeImpl.of(NUMERIC, Optional.empty())))));
    }

    @Test
    public void notify_on_layer_addition() {
        createLayer(LAYER_ID);
        TestSubscriber<Collection<Layer>> sub = TestSubscriber.create();
        store.allLayers().subscribe(sub);

        List<Collection<Layer>> layerEvents = sub.getOnNextEvents();
        assertThat(layerEvents.size(), equalTo(1));
        assertThat(layerEvents.get(0).size(), equalTo(1));
        assertThat(layerEvents.get(0).stream().map(Layer::layerId).collect(toList()),
                containsInAnyOrder(LAYER_ID));

        LayerId layerId2 = LayerId.fromString("777");
        createLayer(layerId2);
        layerEvents = sub.getOnNextEvents();
        assertThat(layerEvents.size(), equalTo(2));
        assertThat(layerEvents.get(1).size(), equalTo(2));
        assertThat(layerEvents.get(1).stream().map(Layer::layerId).collect(toList()),
                containsInAnyOrder(LAYER_ID, layerId2));
    }

     @Test
    public void notify_on_layer_deletion() {
         createLayer(LAYER_ID);
         TestSubscriber<Collection<Layer>> sub = TestSubscriber.create();
         store.allLayers().subscribe(sub);

         assertThat(sub.getOnNextEvents().size(), equalTo(1));
         assertThat(sub.getOnNextEvents().get(0).size(), equalTo(1));
         store.deleteLayer(LAYER_ID);
         assertThat(sub.getOnNextEvents().size(), equalTo(2));
         assertThat(sub.getOnNextEvents().get(1).size(), equalTo(0));
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
        final Action1<SourceUpdate> sub = createLayer(LAYER_ID, indexable);

        Observable.just(
                updateFor(modelFeature("a"))
        ).subscribe(sub);

        final Layer layer = store.getLayer(LAYER_ID).get();

        assertThat(layer.indexedFeatures(), hasSize(size));
    }

    private SourceUpdate updateFor(NakedFeature... features) {
        return SourceUpdateImpl.of(asList(features));
    }

    private NakedFeature modelFeature(String externalId) {
        return NakedFeatureImpl.of(
                externalId,
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

    private Action1<SourceUpdate> createLayer(LayerId id) {
        return createLayer(id, true);
    }

    private Action1<SourceUpdate> createLayer(LayerId id, boolean indexable) {
        return store.createLayer(id, metadata("foo", "bar"), IDENTITY_VIEW, schema("blah"), indexable);
    }

    private AttributeSchema schema(String blessed) {
        return AttributeSchemaImpl.builder().blessedAttribute(AttributeNameImpl.of(blessed)).build();
    }

    private LayerMetadata metadata(String name, String description) {
        return LayerMetadataImpl.of(name, description, Optional.empty(), Optional.empty());
    }

}
