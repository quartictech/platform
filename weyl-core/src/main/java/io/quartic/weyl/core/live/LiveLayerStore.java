package io.quartic.weyl.core.live;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private static final Logger log = LoggerFactory.getLogger(LiveLayerStore.class);

    private final Map<LayerId, Layer<Geometry>> layers = Maps.newHashMap();

    public LayerId createLayer(LayerMetadata metadata) {
        final LayerId layerId = LayerId.of(UUID.randomUUID().toString());
        layers.put(layerId, ImmutableRawLayer.<Geometry>builder()
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
                                f.geometry(),
                                f.metadata().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().get()))
                        ))
                        .collect(Collectors.toList()));
    }

    public void addToLayer(LayerId layerId, FeatureCollection features) {
        checkLayerExists(layerId);

        // TODO: validate that all entries are of type Point

        final Collection<io.quartic.weyl.core.model.Feature<Geometry>> target = layers.get(layerId).features();
        features.features()
                .stream()
                .map(f -> ImmutableFeature.of(
                        f.id().get(), // TODO - what if empty?  (Shouldn't be, because we validate in LayerResource)
                        f.geometry(),
                        f.properties().entrySet()
                                .stream()
                                .collect(toMap(Map.Entry::getKey, e -> Optional.of(e.getValue())))
                ))
                .collect(Collectors.toCollection(() -> target));
    }

    private void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.id());
    }
}
