package io.quartic.weyl.core;

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.live.*;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.common.uid.SequenceUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import static io.quartic.weyl.core.live.LayerView.IDENTITY_VIEW;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class LayerStoreShould {
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

        store.createLayer(id1, lm1, IDENTITY_VIEW);
        store.createLayer(id2, lm2, IDENTITY_VIEW);

        final Collection<AbstractLayer> layers = store.listLayers();

        assertThat(layers.stream().map(AbstractLayer::layerId).collect(toList()),
                containsInAnyOrder(id1, id2));
        assertThat(layers.stream().map(AbstractLayer::metadata).collect(toList()),
                containsInAnyOrder(lm1, lm2));
        assertThat(layers.stream().map(AbstractLayer::live).collect(toList()),
                containsInAnyOrder(false, false));
    }

    @Test
    public void not_list_layer_once_deleted() throws Exception {
        LayerId id = createLayer();
        store.deleteLayer(id);

        assertThat(store.listLayers(), empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_adding_to_non_existent_layer() throws Exception {
        LiveImporter importer = importerFor(feature("a", "1"));

        store.addToLayer(LayerId.of("666"), importer);
    }

    @Test
    public void accept_if_adding_to_existing_layer() throws Exception {
        LayerId id = createLayer();
        LiveImporter importer = importerFor(feature("a", "1"));

        int num = store.addToLayer(id, importer);

        assertThat(num, equalTo(1));

        final Collection<AbstractLayer> layers = store.listLayers();
        assertThat(layers.stream().map(AbstractLayer::live).collect(toList()),
                containsInAnyOrder(true));
    }

    @Test
    public void notify_subscribers_of_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LayerState> subscriber = mock(Consumer.class);
        LiveImporter importer = importerFor(feature("a", "1"));

        store.addSubscriber(id, subscriber);
        store.addToLayer(id, importer);

        final LayerState layerState = captureLiveLayerState(subscriber);
        assertThat(layerState.featureCollection(),
                containsInAnyOrder(feature("a", "1")));
        assertThat(layerState.schema(),
                equalTo(ImmutableAttributeSchema.builder()
                        .attributes(ImmutableMap.of("timestamp", Attribute.of(NUMERIC, Optional.empty())))
                        .build()
                ));
    }

    @Test
    public void notify_subscribers_of_extra_features_added_to_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LayerState> subscriber = mock(Consumer.class);

        store.addToLayer(id, importerFor(feature("a", "1")));
        store.addSubscriber(id, subscriber);
        store.addToLayer(id, importerFor(feature("b", "2")));

        assertThat(captureLiveLayerState(subscriber).featureCollection(),
                containsInAnyOrder(
                        feature("a", "1"),
                        feature("b", "2")
                ));
    }

    @Test
    public void update_metadata_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        LayerMetadata newMetadata = metadata("cheese", "monkey");
        store.createLayer(id, newMetadata, IDENTITY_VIEW);

        final Collection<AbstractLayer> layers = store.listLayers();

        assertThat(layers.stream().map(AbstractLayer::metadata).collect(toList()),
                containsInAnyOrder(newMetadata));
    }

    @Test
    public void not_delete_layer_contents_if_create_called_on_the_same_layer() throws Exception {
        LayerId id = createLayer();
        Consumer<LayerState> subscriber = mock(Consumer.class);

        store.addToLayer(id, importerFor(feature("a", "1")));

        createLayer();  // Create again
        store.addSubscriber(id, subscriber);
        store.addToLayer(id, importerFor(feature("b", "2")));

        assertThat(captureLiveLayerState(subscriber).featureCollection(),
                containsInAnyOrder(
                        feature("a", "1"),
                        feature("b", "2")
                ));
    }

    @Test
    public void notify_listeners_on_change() throws Exception {
        LayerStoreListener listenerA = mock(LayerStoreListener.class);
        LayerStoreListener listenerB = mock(LayerStoreListener.class);

        LayerId id = createLayer();
        store.addListener(listenerA);
        store.addListener(listenerB);
        store.addToLayer(id, importerFor(feature("a", "1")));

        verify(listenerA).onLiveLayerEvent(id, feature("a", "1"));
        verify(listenerB).onLiveLayerEvent(id, feature("a", "1"));
    }

    @Test
    public void not_notify_subscribers_after_unsubscribe() {
        Consumer<LayerState> subscriber = mock(Consumer.class);
        LayerId id = createLayer();

        LayerSubscription subscription = store.addSubscriber(id, subscriber);
        verify(subscriber, times(1)).accept(any());
        store.removeSubscriber(subscription);

        store.addToLayer(id, importerFor(feature("a", "1")));
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void unsubscribe_when_subscriber_deleted() {
        Consumer<LayerState> subscriber = mock(Consumer.class);
        LayerId id = createLayer();
        store.addSubscriber(id, subscriber);
        verify(subscriber, times(1)).accept(any());
        store.deleteLayer(id);
        createLayerWithId(id);
        store.addToLayer(id, importerFor(feature("a", "1")));

        verifyNoMoreInteractions(subscriber);
    }

    private LiveImporter importerFor(Feature... features) {
        LiveImporter importer = mock(LiveImporter.class);
        when(importer.getFeatures()).thenReturn(asList(features));
        return importer;
    }

    private Feature feature(String externalId, String uid) {
        return io.quartic.weyl.core.model.ImmutableFeature.builder()
                .externalId(externalId)
                .uid(FeatureId.of(uid))
                .geometry(factory.createPoint(new Coordinate(123.0, 456.0)))
                .metadata(ImmutableMap.of("timestamp", 1234))
                .build();
    }

    private LayerId createLayer() {
        final LayerId id = LayerId.of("666");
        createLayerWithId(id);
        return id;
    }

    private void createLayerWithId(LayerId id) {
        store.createLayer(id, metadata("foo", "bar"), IDENTITY_VIEW);
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
