package io.quartic.weyl.core.live;

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

    public LiveLayerStore() {
        // TODO: Eliminate this hardcoded layer
        createLayerWithId(
                ImmutableLayerId.of("1234"),
                ImmutableLayerMetadata.builder()
                        .name("Tube stations")
                        .description("Tube station arrivals")
                        .build()
        );
    }

    public void createLayer(LayerMetadata metadata) {
        createLayerWithId(
                ImmutableLayerId.of(UUID.randomUUID().toString()),
                metadata);
    }

    private void createLayerWithId(LayerId id, LayerMetadata metadata) {
        layers.put(id, ImmutableRawLayer.<Geometry>builder()
                .metadata(metadata)
                .schema(ImmutableAttributeSchema.builder().build())
                .features(new FeatureCache())
                .build()
        );
    }

    public Collection<LiveLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(e -> ImmutableLiveLayer.builder()
                        .layerId(e.getKey())
                        .layer(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    public Optional<FeatureCollection> getFeaturesForLayer(LayerId layerId) {
        final Layer<Geometry> layer = layers.get(layerId);
        if (layer == null) {
            return Optional.empty();
        }

        return Optional.of(FeatureCollection.of(
                layer.features()
                        .stream()
                        .map(f -> Feature.of(Optional.of(f.id()), f.geometry(), f.metadata()))
                        .collect(Collectors.toList())));
    }

    public void addToLayer(LayerId id, FeatureCollection features) {
        final Collection<io.quartic.weyl.core.model.Feature<Geometry>> target = layers.get(id).features();
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
}
