package io.quartic.weyl.core.live;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.quartic.weyl.core.AbstractLayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

public class LiveLayerStore extends AbstractLayerStore {
    private final List<LayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LayerSubscription> subscriptions = HashMultimap.create();

    public LiveLayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        super(featureStore, lidGenerator);
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
        subscriptions.removeAll(id);
    }

    // Returns number of features actually added
    public int addToLayer(LayerId layerId, LiveImporter importer) {
        checkLayerExists(layerId);
        final IndexedLayer layer = layers.get(layerId);

        final List<EnrichedFeedEvent> updatedFeedEvents = newArrayList(layer.feedEvents());
        updatedFeedEvents.addAll(importer.getFeedEvents());    // TODO: structural sharing

        final Collection<Feature> newFeatures = importer.getFeatures();
        final io.quartic.weyl.core.feature.FeatureCollection updatedFeatures = layer.layer().features().append(newFeatures);

        putLayer(layer
                .withLayer(layer.layer()
                        .withFeatures(updatedFeatures)
                        .withSchema(createSchema(updatedFeatures))
                )
                .withFeedEvents(updatedFeedEvents)
        );

        notifyListeners(layerId, newFeatures);
        notifySubscribers(layerId);

        return newFeatures.size();
    }

    public void addListener(LayerStoreListener layerStoreListener) {
        listeners.add(layerStoreListener);
    }

    public synchronized LayerSubscription addSubscriber(LayerId layerId, Consumer<LayerState> subscriber) {
        checkLayerExists(layerId);
        LayerSubscription subscription = LayerSubscription.of(layerId, layers.get(layerId).view(), subscriber);
        subscriptions.put(layerId, subscription);
        subscriber.accept(computeLiveLayerState(layers.get(layerId), subscription));
        return subscription;
    }

    public synchronized void removeSubscriber(LayerSubscription layerSubscription) {
        subscriptions.remove(layerSubscription.layerId(), layerSubscription);
    }

    private void notifySubscribers(LayerId layerId) {
        final IndexedLayer layer = layers.get(layerId);
        subscriptions.get(layerId)
                .forEach(subscription -> subscription.subscriber().accept(computeLiveLayerState(layer, subscription)));
    }

    private LayerState computeLiveLayerState(IndexedLayer layer, LayerSubscription subscription) {
        final Collection<Feature> features = layer.layer().features();
        Stream<Feature> computed = subscription.liveLayerView()
                .compute(featureStore.getFeatureIdGenerator(), features);
        return LayerState.builder()
                .schema(layer.layer().schema())
                .featureCollection(computed.collect(toList()))
                .feedEvents(layer.feedEvents())
                .build();
    }

    private void notifyListeners(LayerId layerId, Collection<Feature> newFeatures) {
        newFeatures.forEach(f -> listeners.forEach(listener -> listener.onLiveLayerEvent(layerId, f)));
    }
}
