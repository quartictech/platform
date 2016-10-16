package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class LiveLayerStoreShould {
    private final static LiveLayerView IDENTITY_VIEW = (gen, features) -> features.stream();
    private final FeatureStore featureStore = new FeatureStore(new SequenceUidGenerator<>(FeatureId::of));
    private final LiveLayerStore store = new LiveLayerStore(featureStore);

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = metadata("foo", "bar");
        final LayerMetadata lm2 = metadata("cheese", "monkey");

        LayerId id1 = LayerId.of("666");
        LayerId id2 = LayerId.of("777");

        store.createLayer(id1, lm1, IDENTITY_VIEW);
        store.createLayer(id2, lm2, IDENTITY_VIEW);

        final Collection<IndexedLayer> layers = store.listLayers();

        assertThat(layers.stream().map(IndexedLayer::layerId).collect(toList()),
                containsInAnyOrder(id1, id2));
        assertThat(layers.stream().map(l -> l.layer().metadata()).collect(toList()),
                containsInAnyOrder(lm1, lm2));
    }

    @Test
    public void not_list_layer_once_deleted() throws Exception {
        LayerId id = createLayer();
        store.deleteLayer(id);

        assertThat(store.listLayers(), empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_adding_to_non_existent_layer() throws Exception {
        store.addToLayer(LayerId.of("666"), liveEvents(feature("a", Optional.of(point()))));
    }

    @Test
    public void accept_if_adding_to_existing_layer() throws Exception {
        LayerId id = createLayer();

        int num = store.addToLayer(id, liveEvents(feature("a", Optional.of(point()))));

        assertThat(num, equalTo(1));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        LayerId id = createLayer();

        int num = store.addToLayer(id, liveEvents(feature("a", Optional.empty())));

        assertThat(num, equalTo(0));
    }

    @Test
    public void notify_subscribers_of_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LiveLayerState> subscriber = mock(Consumer.class);
        final Feature feature = feature("a", Optional.of(point()));

        store.addSubscriber(id, subscriber);
        store.addToLayer(id, liveEvents(feature));

        final LiveLayerState liveLayerState = captureLiveLayerState(subscriber);
        assertThat(liveLayerState.featureCollection().features(),
                containsInAnyOrder(featureWithUid("a", "1", point())
                ));
        assertThat(liveLayerState.schema(),
                equalTo(ImmutableAttributeSchema.builder()
                        .attributes(ImmutableMap.of("timestamp", Attribute.of(NUMERIC, Optional.empty())))
                        .build()
                ));
    }

    @Test
    public void notify_subscribers_of_extra_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LiveLayerState> subscriber = mock(Consumer.class);

        store.addToLayer(id, liveEvents(feature("a", Optional.of(point()))));
        store.addSubscriber(id, subscriber);
        store.addToLayer(id, liveEvents(feature("b", Optional.of(point()))));

        assertThat(captureLiveLayerState(subscriber).featureCollection().features(),
                containsInAnyOrder(
                        featureWithUid("a", "1", point()),
                        featureWithUid("b", "2", point())
                ));
    }

    @Test
    public void update_metadata_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        LayerMetadata newMetadata = metadata("cheese", "monkey");
        store.createLayer(id, newMetadata, IDENTITY_VIEW);

        final Collection<IndexedLayer> layers = store.listLayers();

        assertThat(layers.stream().map(l -> l.layer().metadata()).collect(toList()),
                containsInAnyOrder(newMetadata));
    }

    @Test
    public void not_delete_layer_contents_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LiveLayerState> subscriber = mock(Consumer.class);

        store.addToLayer(id, liveEvents(feature("a", Optional.of(point()))));

        createLayer();  // Create again
        store.addSubscriber(id, subscriber);
        store.addToLayer(id, liveEvents(feature("b", Optional.of(point()))));

        assertThat(captureLiveLayerState(subscriber).featureCollection().features(),
                containsInAnyOrder(
                        featureWithUid("a", "1", point()),
                        featureWithUid("b", "2", point())
                ));
    }

    @Test
    public void notify_listeners_on_change() throws Exception {
        LiveLayerStoreListener listenerA = mock(LiveLayerStoreListener.class);
        LiveLayerStoreListener listenerB = mock(LiveLayerStoreListener.class);

        LayerId id = createLayer();
        store.addListener(listenerA);
        store.addListener(listenerB);
        store.addToLayer(id, liveEvents(feature("abcd", Optional.of(point()))));

        final ImmutableFeature feature = ImmutableFeature.builder()
                .externalId("abcd")
                .uid(FeatureId.of("1"))
                .geometry(Utils.toJts(point()))
                .metadata(ImmutableMap.of("timestamp", 1234))
                .build();
        verify(listenerA).onLiveLayerEvent(id, feature);
        verify(listenerB).onLiveLayerEvent(id, feature);
    }

    @Test
    public void not_notify_subscribers_after_unsubscribe() {
        Consumer<LiveLayerState> subscriber = mock(Consumer.class);
        LayerId id = createLayer();

        LiveLayerSubscription subscription = store.addSubscriber(id, subscriber);
        verify(subscriber, times(1)).accept(any());
        store.removeSubscriber(subscription);

        store.addToLayer(id, liveEvents(featureCollection(featureWithUid("a", "1", point()))));
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void unsubscribe_when_subscriber_deleted() {
        Consumer<LiveLayerState> subscriber = mock(Consumer.class);
        LayerId id = createLayer();
        store.addSubscriber(id, subscriber);
        verify(subscriber, times(1)).accept(any());
        store.deleteLayer(id);
        createLayerWithId(id);
        store.addToLayer(id, liveEvents(featureCollection(featureWithUid("a", "1", point()))));

        verifyNoMoreInteractions(subscriber);
    }

    private void createLayerWithId(LayerId id) {
        store.createLayer(id, metadata("foo", "bar"), IDENTITY_VIEW);
    }

    private LayerMetadata metadata(String name, String description) {
        return LayerMetadata.of(name, description, Optional.empty(), Optional.empty());
    }

    private LayerId createLayer() {
        final LayerId id = LayerId.of("666");
        createLayerWithId(id);
        return id;
    }

    private LiveLayerState captureLiveLayerState(Consumer<LiveLayerState> subscriber) {
        ArgumentCaptor<LiveLayerState> captor = ArgumentCaptor.forClass(LiveLayerState.class);
        verify(subscriber, times(2)).accept(captor.capture());
        return captor.getAllValues().get(1);    // Assume first time is initial subscribe
    }

    private Collection<LiveEvent> liveEvents(Feature... features) {
        FeatureCollection featureCollection = FeatureCollection.of(ImmutableList.copyOf(features));
        return liveEvents(featureCollection);
    }

    private Collection<LiveEvent> liveEvents(FeatureCollection featureCollection) {
        LiveEvent liveEvent = LiveEvent.of(Instant.now(), Optional.of(featureCollection), Optional.empty());
        return ImmutableList.of(liveEvent);
    }

    private FeatureCollection featureCollection(Feature... features) {
        return FeatureCollection.of(ImmutableList.copyOf(features));
    }

    private Feature feature(String id, Optional<Geometry> geometry) {
        return Feature.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private Feature featureWithUid(String id, String uid, Geometry geometry) {
        return Feature.of(
                Optional.of(id),
                Optional.of(geometry),
                ImmutableMap.of("timestamp", 1234, "_id", FeatureId.of(uid)));
    }

    private Point point() {
        return point(123.0, 456.0);
    }

    private Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }
}
