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
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public static final LayerId OTHER_LAYER_ID = LayerId.of("777");
    private static final AttributeName ATTRIBUTE_NAME = AttributeName.of("timestamp");
    private final UidGenerator<FeatureId> fidGenerator = SequenceUidGenerator.of(FeatureId::of);
    private final UidGenerator<LayerId> lidGenerator = SequenceUidGenerator.of(LayerId::of);
    private final FeatureStore featureStore = new FeatureStore(fidGenerator);
    private final LayerStore store = new LayerStore(featureStore, lidGenerator);
    private final GeometryFactory factory = new GeometryFactory();

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = metadata("foo", "bar");
        final LayerMetadata lm2 = metadata("cheese", "monkey");

        final AttributeSchema as1 = schema("foo");
        final AttributeSchema as2 = schema("bar");

        store.createLayer(LAYER_ID, lm1, IDENTITY_VIEW, as1, true);
        store.createLayer(OTHER_LAYER_ID, lm2, IDENTITY_VIEW, as2, true);

        final Collection<AbstractLayer> layers = store.listLayers();

        assertThat(layers.stream().map(AbstractLayer::layerId).collect(toList()),
                containsInAnyOrder(LAYER_ID, OTHER_LAYER_ID));
        assertThat(layers.stream().map(AbstractLayer::metadata).collect(toList()),
                containsInAnyOrder(lm1, lm2));
        assertThat(layers.stream().map(AbstractLayer::indexable).collect(toList()),
                containsInAnyOrder(true, true));
        assertThat(layers.stream().map(AbstractLayer::schema).collect(toList()),
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
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);

        Observable.just(updateFor(feature("a", "1"))).subscribe(sub);

        final AbstractLayer layer = store.getLayer(LAYER_ID).get();
        assertThat(layer.schema().blessedAttributes(), Matchers.contains(AttributeName.of("blah")));
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
                equalTo(schema("blah").withAttributes(ImmutableMap.of(ATTRIBUTE_NAME, Attribute.of(NUMERIC, Optional.empty())))));
    }

    @Test
    public void handle_concurrent_subscription_changes() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);
        final DoOnTrigger onTrigger = new DoOnTrigger(() -> store.addSubscriber(LAYER_ID, mock(Consumer.class)));    // Emulate concurrent subscription change

        Consumer<LayerState> subscriber = mock(Consumer.class);
        store.addSubscriber(LAYER_ID, subscriber);
        store.addSubscriber(LAYER_ID, mock(Consumer.class));

        doAnswer(invocation -> {
            onTrigger.trigger();
            Thread.sleep(30);
            return null;
        }).when(subscriber).accept(any());

        assertCanRunToCompletion(sub);
    }

    @Test
    public void handle_concurrent_listener_changes() throws Exception {
        final Subscriber<SourceUpdate> sub = createLayer(LAYER_ID);
        final DoOnTrigger onTrigger = new DoOnTrigger(() -> store.addListener(mock(LayerStoreListener.class)));    // Emulate concurrent subscription change

        LayerStoreListener listener = mock(LayerStoreListener.class);
        store.addListener(listener);
        store.addListener(mock(LayerStoreListener.class));

        doAnswer(invocation -> {
            onTrigger.trigger();
            Thread.sleep(30);
            return null;
        }).when(listener).onLiveLayerEvent(any(), any());

        assertCanRunToCompletion(sub);
    }

    private void assertCanRunToCompletion(Subscriber<SourceUpdate> sub) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Observable.just(updateFor(feature("a", "1")), updateFor(feature("b", "2")))
                .doOnCompleted(() -> completed.set(true))
                .subscribe(sub);

        assertThat(completed.get(), equalTo(true)); // This won't be complete if there was an error
    }

    private static class DoOnTrigger {
        private final CountDownLatch latch = new CountDownLatch(1);

        public DoOnTrigger(Runnable runnable) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runnable.run();
            }).start();
        }

        public void trigger() {
            latch.countDown();
        }
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
                .metadata(ImmutableMap.of(ATTRIBUTE_NAME, 1234))
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
        return store.createLayer(id, metadata("foo", "bar"), IDENTITY_VIEW, schema("blah"), indexable);
    }

    private AttributeSchema schema(String blessed) {
        return AttributeSchema.builder().blessedAttribute(AttributeName.of(blessed)).build();
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
