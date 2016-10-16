package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import io.quartic.weyl.core.attributes.AttributeSchemaInferrer;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.SequenceUidGenerator;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.LayerStore.spatialIndex;
import static io.quartic.weyl.core.StatsCalculator.calculateStats;

public class LiveLayerStore {
    private final FeatureStore featureStore;
    private final Map<LayerId, IndexedLayer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = newArrayList();
    private final Multimap<LayerId, LiveLayerSubscription> liveLayerSubscriptions = HashMultimap.create();
    private final UidGenerator<LiveEventId> eidGenerator = new SequenceUidGenerator<>(LiveEventId::of);

    public LiveLayerStore(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    public void createLayer(LayerId id, LayerMetadata metadata, LiveLayerView view) {
        if (layers.containsKey(id)) {
            final IndexedLayer old = layers.get(id);
            putLayer(old.withLayer(old.layer()
                    .withMetadata(metadata))
                    .withView(view)
            );
        } else {
            io.quartic.weyl.core.feature.FeatureCollection features = featureStore.newCollection();

            Layer layer = Layer.builder()
                    .metadata(metadata)
                    .schema(createSchema(features))
                    .features(features)
                    .build();

            putLayer(index(id, layer, view));
        }
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
        liveLayerSubscriptions.removeAll(id);
    }

    public Collection<IndexedLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // Returns number of features actually added
    public int addToLayer(LayerId layerId, Collection<LiveEvent> events) {
        checkLayerExists(layerId);

        final IndexedLayer layer = layers.get(layerId);

        final LiveImporter importer = new LiveImporter(events, featureStore.getFeatureIdGenerator(), eidGenerator);

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

    private ImmutableAttributeSchema createSchema(io.quartic.weyl.core.feature.FeatureCollection features) {
        return ImmutableAttributeSchema.builder()
                .attributes(AttributeSchemaInferrer.inferSchema(features))
                .primaryAttribute(Optional.empty())
                .build();
    }

    private void putLayer(IndexedLayer layer) {
        layers.put(layer.layerId(), layer);
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

    private IndexedLayer index(LayerId layerId, Layer layer, LiveLayerView view) {
        Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

        return IndexedLayer.builder()
                .layer(layer)
                .spatialIndex(spatialIndex(features))
                .indexedFeatures(features)
                .layerId(layerId)
                .layerStats(calculateStats(layer))
                .feedEvents(ImmutableList.of())     // TODO
                .view(view)
                .build();
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    private static Map<String, Object> convertMetadata(FeatureId featureId, Map<String, Object> metadata) {
        final Map<String, Object> output = Maps.newHashMap(metadata);
        output.put("_id", featureId);  // TODO: eliminate the _id concept
        return output;
    }
}
