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

import java.util.*;
import java.util.stream.Collectors;

import static io.quartic.weyl.core.utils.Utils.uuid;
import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LiveLayerStore.class);
    private final Map<LayerId, Layer> layers = Maps.newHashMap();
    private final List<LiveLayerStoreListener> listeners = Lists.newArrayList();

    public LayerId createLayer(LayerMetadata metadata) {
        final LayerId layerId = uuid(LayerId::of);
        layers.put(layerId, ImmutableRawLayer.builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(new FeatureCache())
                .build()
        );
        return layerId;
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

        final Collection<io.quartic.weyl.core.model.Feature> target = layers.get(layerId).features();
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
}
