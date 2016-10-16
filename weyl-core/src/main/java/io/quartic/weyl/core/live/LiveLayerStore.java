package io.quartic.weyl.core.live;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.quartic.weyl.core.AbstractLayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;

public class LiveLayerStore extends AbstractLayerStore {
    private final List<LiveLayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LiveLayerSubscription> liveLayerSubscriptions = HashMultimap.create();

    public LiveLayerStore(FeatureStore featureStore) {
        super(featureStore);
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
        liveLayerSubscriptions.removeAll(id);
    }

    // Returns number of features actually added
    public int addToLayer(LayerId layerId, LiveImporter importer) {
        checkLayerExists(layerId);
        final IndexedLayer layer = layers.get(layerId);

        final List<EnrichedFeedEvent> updatedFeedEvents = newArrayList(layer.feedEvents());
        updatedFeedEvents.addAll(importer.getFeedEvents());    // TODO: structural sharing

        final Collection<io.quartic.weyl.core.model.Feature> newFeatures = importer.getFeatures();
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

    public void addListener(LiveLayerStoreListener liveLayerStoreListener) {
        listeners.add(liveLayerStoreListener);
    }

    public synchronized LiveLayerSubscription addSubscriber(LayerId layerId, Consumer<LiveLayerState> subscriber) {
        checkLayerExists(layerId);
        LiveLayerSubscription subscription = LiveLayerSubscription.of(layerId, layers.get(layerId).view(), subscriber);
        liveLayerSubscriptions.put(layerId, subscription);
        subscriber.accept(computeLiveLayerState(layers.get(layerId), subscription));
        return subscription;
    }

    public synchronized void removeSubscriber(LiveLayerSubscription liveLayerSubscription) {
        liveLayerSubscriptions.remove(liveLayerSubscription.layerId(), liveLayerSubscription);
    }

    private void notifySubscribers(LayerId layerId) {
        final IndexedLayer layer = layers.get(layerId);
        liveLayerSubscriptions.get(layerId)
                .forEach(subscription -> subscription.subscriber().accept(computeLiveLayerState(layer, subscription)));
    }

    private LiveLayerState computeLiveLayerState(IndexedLayer layer, LiveLayerSubscription subscription) {
        final Collection<io.quartic.weyl.core.model.Feature> features = layer.layer().features();
        Stream<io.quartic.weyl.core.model.Feature> computed = subscription.liveLayerView()
                .compute(featureStore.getFeatureIdGenerator(), features);
        FeatureCollection featureCollection = FeatureCollection.of(
                computed
                        .map(this::fromJts)
                        .collect(Collectors.toList()));
        return LiveLayerState.builder()
                .schema(layer.layer().schema())
                .featureCollection(featureCollection)
                .feedEvents(layer.feedEvents())
                .build();
    }

    private void notifyListeners(LayerId layerId, Collection<io.quartic.weyl.core.model.Feature> newFeatures) {
        newFeatures.forEach(f -> listeners.forEach(listener -> listener.onLiveLayerEvent(layerId, f)));
    }

    private Feature fromJts(io.quartic.weyl.core.model.Feature f) {
        return Feature.of(
                Optional.of(f.externalId()),
                Optional.of(Utils.fromJts(f.geometry())),
                convertMetadata(f.uid(), f.metadata())
        );
    }

    private static Map<String, Object> convertMetadata(FeatureId featureId, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap(metadata);
        output.put("_id", featureId);  // TODO: eliminate the _id concept
        return output;
    }
}
