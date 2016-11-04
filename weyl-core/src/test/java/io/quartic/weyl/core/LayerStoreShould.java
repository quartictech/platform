package io.quartic.weyl.core;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.model.FeedEvent;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.live.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.source.SourceUpdate;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class LayerStoreShould {
    public static final Instant INSTANT = Instant.now();
    public static final LayerId LAYER_ID = LayerId.of("666");
    private final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerId::of);
    private final FeatureStore featureStore = new FeatureStore(fidGenerator);
    private final LayerStore store = new LayerStore(featureStore, lidGenerator);
    private final GeometryFactory factory = new GeometryFactory();

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = metadata("foo", "bar");
        final LayerMetadata lm2 = metadata("cheese", "monkey");

        LayerId id1 = LayerId.of("666");
        LayerId id2 = LayerId.of("777");

        store.createLayer(id1, lm1, true, IDENTITY_VIEW);
        store.createLayer(id2, lm2, true, IDENTITY_VIEW);

        final Collection<AbstractLayer> layers = store.listLayers();

        assertThat(layers.stream().map(AbstractLayer::layerId).collect(toList()),
                containsInAnyOrder(id1, id2));
        assertThat(layers.stream().map(AbstractLayer::metadata).collect(toList()),
                containsInAnyOrder(lm1, lm2));
        assertThat(layers.stream().map(AbstractLayer::indexable).collect(toList()),
                containsInAnyOrder(true, true));
    }

    @Test
    public void not_list_layer_once_deleted() throws Exception {
        createLayer(LAYER_ID);
        store.deleteLayer(LAYER_ID);

        assertThat(store.listLayers(), empty());
    }

    @Test
    public void add_observed_features_and_events_to_layer() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        Observable.just(
                updateFor(newArrayList(feature("a", "1")), newArrayList(event("789"))),
                updateFor(newArrayList(feature("b", "2")), newArrayList(event("987")))
        ).subscribe(sub);

        final AbstractLayer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.features(),
                containsInAnyOrder(feature("a", "1"), feature("b", "2")));
        assertThat(layer.feedEvents(),
                containsInAnyOrder(event("789"), event("987")));
    }

    @Test
    public void notify_subscribers_of_observed_features_and_events() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        Consumer<LayerState> subscriber = mock(Consumer.class);
        store.addSubscriber(LAYER_ID, subscriber);

        Observable.just(
                updateFor(newArrayList(feature("a", "1")), newArrayList(event("789")))
        ).subscribe(sub);

        final LayerState layerState = captureLiveLayerState(subscriber);
        assertThat(layerState.featureCollection(),
                containsInAnyOrder(feature("a", "1")));
        assertThat(layerState.feedEvents(),
                containsInAnyOrder(event("789")));
        assertThat(layerState.schema(),
                equalTo(ImmutableAttributeSchema.builder()
                        .attributes(ImmutableMap.of("timestamp", Attribute.of(NUMERIC, Optional.empty())))
                        .build()
                ));
    }

    // TODO: using subjects is kind of gross (see e.g. http://tomstechnicalblog.blogspot.co.uk/2016/03/rxjava-problem-with-subjects.html)
    // Luckily, this should go away once we model downstream stuff reactively too
    @Test
    public void notify_subscribers_of_all_features_upon_subscribing() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        PublishSubject<SourceUpdate> subject = PublishSubject.create();
        subject.subscribe(sub);

        subject.onNext(updateFor(feature("a", "1")));   // Observed before

        Consumer<LayerState> subscriber = mock(Consumer.class);
        store.addSubscriber(LAYER_ID, subscriber);

        subject.onNext(updateFor(feature("b", "2")));   // Observed after

        assertThat(captureLiveLayerState(subscriber).featureCollection(),
                containsInAnyOrder(
                        feature("a", "1"),
                        feature("b", "2")
                ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_create_called_on_an_existing_layer() throws Exception {
        createLayer(LAYER_ID);
        createLayer(LAYER_ID);
    }



    @Test
    public void notify_listeners_on_change() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        LayerStoreListener listenerA = mock(LayerStoreListener.class);
        LayerStoreListener listenerB = mock(LayerStoreListener.class);
        store.addListener(listenerA);
        store.addListener(listenerB);

        Observable.just(
                updateFor(feature("a", "1"))
        ).subscribe(sub);

        verify(listenerA).onLiveLayerEvent(LAYER_ID, feature("a", "1"));
        verify(listenerB).onLiveLayerEvent(LAYER_ID, feature("a", "1"));
    }

    @Test
    public void not_notify_subscribers_after_unsubscribe() {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        Consumer<LayerState> subscriber = mock(Consumer.class);
        LayerSubscription subscription = store.addSubscriber(LAYER_ID, subscriber);
        verify(subscriber, times(1)).accept(any());
        store.removeSubscriber(subscription);

        Observable.just(
                updateFor(feature("a", "1"))
        ).subscribe(sub);

        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void unsubscribe_when_subscriber_deleted() {
        createLayer(LAYER_ID);

        Consumer<LayerState> subscriber = mock(Consumer.class);
        store.addSubscriber(LAYER_ID, subscriber);
        verify(subscriber, times(1)).accept(any());

        // Delete and recreate
        store.deleteLayer(LAYER_ID);
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        Observable.just(
                updateFor(feature("a", "1"))
        ).subscribe(sub);

        verifyNoMoreInteractions(subscriber);
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
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID, indexable);

        Observable.just(
                updateFor(feature("a", "1"))
        ).subscribe(sub);

        final AbstractLayer layer = store.getLayer(LAYER_ID).get();

        assertThat(layer.indexedFeatures(), hasSize(size));
    }

    private SourceUpdate updateFor(Feature... features) {
        return updateFor(asList(features), newArrayList());
    }

    private SourceUpdate updateFor(List<Feature> features, List<EnrichedFeedEvent> events) {
        return SourceUpdate.of(features, events);
    }

    private Feature feature(String externalId, String uid) {
        return io.quartic.weyl.core.model.ImmutableFeature.builder()
                .externalId(externalId)
                .uid(FeatureId.of(uid))
                .geometry(factory.createPoint(new Coordinate(123.0, 456.0)))
                .metadata(ImmutableMap.of("timestamp", 1234))
                .build();
    }

    private EnrichedFeedEvent event(String id) {
        return EnrichedFeedEvent.builder()
                .id(LiveEventId.of(id))
                .feedEvent(FeedEvent.of("foo", "bar", emptyMap()))
                .timestamp(INSTANT)
                .build();
    }

    private Subscriber<SourceUpdate> createLayer(LayerId id) {
        return createLayer(id, true);
    }

    private Subscriber<SourceUpdate> createLayer(LayerId id, boolean indexable) {
        return store.createLayer(id, metadata("foo", "bar"), indexable, IDENTITY_VIEW);
    }

    private LayerMetadata metadata(String name, String description) {
        return LayerMetadata.of(name, description, Optional.empty(), Optional.empty());
    }

    private LayerState captureLiveLayerState(Consumer<LayerState> subscriber) {
        ArgumentCaptor<LayerState> captor = ArgumentCaptor.forClass(LayerState.class);
        verify(subscriber, times(2)).accept(captor.capture());
        return captor.getAllValues().get(1);    // Assume first time is initial subscribe
    }
}
