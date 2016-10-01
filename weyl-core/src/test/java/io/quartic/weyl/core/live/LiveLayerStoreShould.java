package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.*;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class LiveLayerStoreShould {
    private final LiveLayerStore store = new LiveLayerStore();

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = LayerMetadata.of("foo", "bar");
        final LayerMetadata lm2 = LayerMetadata.of("cheese", "monkey");

        LayerId id1 = LayerId.of("abc");
        LayerId id2 = LayerId.of("def");

        store.createLayer(id1, lm1, LiveLayerViewType.LOCATION_AND_TRACK);
        store.createLayer(id2, lm2, LiveLayerViewType.LOCATION_AND_TRACK);

        final Collection<LiveLayer> layers = store.listLayers();

        assertThat(layers.stream().map(LiveLayer::layerId).collect(toList()),
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
        store.addToLayer(LayerId.of("abcd"), featureCollection(feature("a", point())));
    }

    @Test
    public void accept_if_adding_to_existing_layer() throws Exception {
        LayerId id = createLayer();

        store.addToLayer(id, featureCollection(featureWithId("a", point())));
    }

    @Test
    public void notify_subscribers_of_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<FeatureCollection> subscriber = mock(Consumer.class);

        store.subscribeView(id, subscriber);
        store.addToLayer(id, featureCollection(feature("a", point())));

        verify(subscriber).accept(
                featureCollection(
                        featureWithId("a", point())
                ));
    }

    @Test
    public void notify_subscribers_of_extra_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<FeatureCollection> subscriber = mock(Consumer.class);

        store.addToLayer(id, featureCollection(feature("a", point())));
        store.subscribeView(id, subscriber);
        store.addToLayer(id, featureCollection(feature("b", point())));

        verify(subscriber).accept(
                featureCollection(
                        featureWithId("a", point()),
                        featureWithId("b", point())
                ));
    }

    @Test
    public void update_metadata_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        LayerMetadata newMetadata = LayerMetadata.of("cheese", "monkey");
        store.createLayer(id, newMetadata, LiveLayerViewType.LOCATION_AND_TRACK);

        final Collection<LiveLayer> layers = store.listLayers();

        assertThat(layers.stream().map(l -> l.layer().metadata()).collect(toList()),
                containsInAnyOrder(newMetadata));
    }

    @Test
    public void not_delete_layer_contents_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<FeatureCollection> subscriber = mock(Consumer.class);

        store.subscribeView(id, subscriber);
        store.addToLayer(id, featureCollection(feature("a", point())));
        createLayer();

        verify(subscriber).accept(
                featureCollection(
                        featureWithId("a", point())
                ));
    }

    @Test
    public void notify_listeners_on_change() throws Exception {
        LiveLayerStoreListener listenerA = mock(LiveLayerStoreListener.class);
        LiveLayerStoreListener listenerB = mock(LiveLayerStoreListener.class);

        LayerId id = createLayer();
        store.addListener(listenerA);
        store.addListener(listenerB);
        store.addToLayer(id, featureCollection(feature("a", point())));

        final ImmutableFeature feature = ImmutableFeature.of("a", Utils.toJts(point()), ImmutableMap.of("timestamp", Optional.of(1234)));
        verify(listenerA).liveLayerEvent(id, feature);
        verify(listenerB).liveLayerEvent(id, feature);
    }

    @Test
    public void not_notify_subscribers_after_unsubscribe() {
        Consumer<FeatureCollection> subscriber = mock(Consumer.class);
        LayerId id = createLayer();
        LiveLayerSubscription subscription = store.subscribeView(id, subscriber);
        store.unsubscribeView(subscription);

        store.addToLayer(id, featureCollection(featureWithId("a", point())));

        verifyZeroInteractions(subscriber);
    }

    @Test
    public void unsubscribe_after_subscriber_deleted() {
        Consumer<FeatureCollection> subscriber = mock(Consumer.class);
        LayerId id = createLayer();
        store.subscribeView(id, subscriber);
        store.deleteLayer(id);
        createLayerWithId(id);
        store.addToLayer(id, featureCollection(featureWithId("a", point())));

        verifyZeroInteractions(subscriber);
    }

    private void createLayerWithId(LayerId id) {
        store.createLayer(id, LayerMetadata.of("foo", "bar"), LiveLayerViewType.LOCATION_AND_TRACK);
    }

    private LayerId createLayer() {
        final LayerId id = LayerId.of("abc");
        createLayerWithId(id);
        return id;
    }

    private FeatureCollection featureCollection(Feature... features) {
        return FeatureCollection.of(ImmutableList.copyOf(features));
    }

    private Feature feature(String id, Geometry geometry) {
        return Feature.of(Optional.of(id), geometry, ImmutableMap.of("timestamp", 1234));
    }

    private Feature featureWithId(String id, Geometry geometry) {
        return Feature.of(Optional.of(id), geometry, ImmutableMap.of("timestamp", 1234, "_id", id));
    }

    private Point point() {
        return point(123.0, 456.0);
    }

    private Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }
}
