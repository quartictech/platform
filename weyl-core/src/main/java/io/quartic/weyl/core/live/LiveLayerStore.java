package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LiveLayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = Lists.newArrayList();

    public void createLayer(LayerId id, LayerMetadata metadata) {
        Collection<AbstractFeature> features
                = layers.containsKey(id)
                ? layers.get(id).features()
                : new FeatureCache();

        layers.put(id, ImmutableRawLayer.builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(features)
                .build()
        );
    }

    public void deleteLayer(LayerId id) {
        checkLayerExists(id);
        layers.remove(id);
    }

    public Collection<LiveLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(e -> LiveLayer.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public FeatureCollection getFeaturesForLayer(LayerId layerId) {
        checkLayerExists(layerId);

        return FeatureCollection.of(
                layers.get(layerId).features()
                        .stream()
                        .map(f -> Feature.of(
                                Optional.of(f.id()),
                                Utils.fromJts(f.geometry()),
                                convertMetadata(f.id(), f.metadata())
                        ))
                        .collect(Collectors.toList()));
    }

    private Map<String, Object> convertMetadata(FeatureId id, Map<String, Optional<Object>> metadata) {
        final Map<String, Object> output = metadata.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().get()));
        output.put("_id", id);
        return output;
    }

    public void addToLayer(LayerId layerId, FeatureCollection features) {
        checkLayerExists(layerId);

        // TODO: validate that all entries are of type Point

        final Collection<AbstractFeature> target = layers.get(layerId).features();
        final Collection<AbstractFeature> newFeatures = features.features()
                .stream()
                .map(f -> io.quartic.weyl.core.model.Feature.of(
                        f.id().get(), // TODO - what if empty?  (Shouldn't be, because we validate in LayerResource)
                        Utils.toJts(f.geometry()),
                        f.properties().entrySet()
                                .stream()
                                .collect(toMap(Map.Entry::getKey, e -> Optional.of(e.getValue())))
                ))
                .collect(Collectors.toList());

        newFeatures.forEach(f -> notifyListeners(layerId, f));
        newFeatures.stream().collect(Collectors.toCollection(() -> target));
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.id());
    }

    public void addListener(LiveLayerStoreListener liveLayerStoreListener) {
        listeners.add(liveLayerStoreListener);
    }

    private void notifyListeners(LayerId layerId, AbstractFeature feature) {
        listeners.forEach(listener -> listener.liveLayerEvent(layerId, feature));
    }
}
