package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LiveLayerStore.class);
    private final Map<LayerId, LiveLayer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = Lists.newArrayList();
    private final Multimap<LayerId, LiveLayerSubscription> liveLayerSubscriptions = HashMultimap.create();

    public void createLayer(LayerId id, LayerMetadata metadata, LiveLayerViewType viewType) {
        Collection<io.quartic.weyl.core.model.Feature> features
                = layers.containsKey(id)
                ? layers.get(id).layer().features()
                : new FeatureCache();

        Layer layer = ImmutableRawLayer.builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(features)
                .build();

        layers.put(id, LiveLayer.of(id, layer, viewType));
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
    }

    public Collection<LiveLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public FeatureCollection getFeaturesForLayer(LayerId layerId) {
        checkLayerExists(layerId);

        return FeatureCollection.of(
                layers.get(layerId).layer().features()
                        .stream()
                        .map(f -> Feature.of(Optional.of(
                                f.id()),
                                Utils.fromJts(f.geometry()),
                                convertMetadata(f.id(), f.metadata())
                        ))
                        .collect(Collectors.toList()));
    }

    private Map<String, Object> convertMetadata(String id, Map<String, Optional<Object>> metadata) {
        final Map<String, Object> output = metadata.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().get()));
        output.put("_id", id);
        return output;
    }

    public void addToLayer(LayerId layerId, FeatureCollection features) {
        checkLayerExists(layerId);

        // TODO: validate that all entries are of type Point

        final Collection<io.quartic.weyl.core.model.Feature> target = layers.get(layerId).layer().features();
        final Collection<io.quartic.weyl.core.model.Feature> newFeatures = features.features()
                .stream()
                .map(f -> ImmutableFeature.of(
                        f.id().get(), // TODO - what if empty?  (Shouldn't be, because we validate in LayerResource)
                        Utils.toJts(f.geometry()),
                        f.properties().entrySet()
                                .stream()
                                .collect(toMap(Map.Entry::getKey, e -> Optional.of(e.getValue())))
                ))
                .collect(Collectors.toList());

        newFeatures.forEach(f -> notifyListeners(layerId, f));
        newFeatures.stream().collect(Collectors.toCollection(() -> target));

        liveLayerSubscriptions.get(layerId)
                .forEach(subscription -> {
                    Stream<io.quartic.weyl.core.model.Feature> featureStream = subscription.liveLayerView().compute(layers.get(layerId).layer().features());
                    FeatureCollection featureCollection = FeatureCollection.of(
                        featureStream
                                .map(f -> Feature.of(Optional.of(
                                        f.id()),
                                        Utils.fromJts(f.geometry()),
                                        convertMetadata(f.id(), f.metadata())
                                ))
                                .collect(Collectors.toList()));
                    subscription.subscriber().accept(featureCollection);
                });
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.id());
    }

    public void addListener(LiveLayerStoreListener liveLayerStoreListener) {
        listeners.add(liveLayerStoreListener);
    }

    private void notifyListeners(LayerId layerId, io.quartic.weyl.core.model.Feature feature) {
        listeners.forEach(listener -> listener.liveLayerEvent(layerId, feature));
    }

    public synchronized LiveLayerSubscription subscribeView(LayerId layerId, Consumer<FeatureCollection> subscriber) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.id());
        LiveLayerSubscription subscription = LiveLayerSubscription.of(layerId, layers.get(layerId).viewType().getLiveLayerView(), subscriber);
        liveLayerSubscriptions.put(layerId, subscription);
        return subscription;
    }

    public synchronized void unsubscribeView(LiveLayerSubscription liveLayerSubscription) {
       liveLayerSubscriptions.remove(liveLayerSubscription.layerId(), liveLayerSubscription);
    }
}
