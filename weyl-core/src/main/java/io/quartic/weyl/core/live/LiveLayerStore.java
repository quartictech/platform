package io.quartic.weyl.core.live;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class LiveLayerStore {
    private final Map<LayerId, Layer<Geometry>> layers = Maps.newHashMap();

    private final List<io.quartic.weyl.core.model.Feature<Geometry>> features = Lists.newArrayList();

    public LiveLayerStore() {
        layers.put(ImmutableLayerId.of("1234"), createFakeLayer());
    }

    public Optional<AbstractFeatureCollection> getFeaturesForLayer(LayerId layerId) {
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

    public Collection<LiveLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(e -> ImmutableLiveLayer.builder().layerId(e.getKey()).layer(e.getValue()).build())
                .collect(Collectors.toList());
    }

    public void addToLayer(Feature feature) {
        features.add(ImmutableFeature.of(
                feature.id().get(), // TODO - what if empty?
                feature.geometry(),
                feature.properties().entrySet().stream().collect(toMap(Map.Entry::getKey, Optional::of))));
    }

    private Layer<Geometry> createFakeLayer() {
        return ImmutableRawLayer.<Geometry>builder()
                .metadata(ImmutableLayerMetadata.builder()
                        .name("Tube stations")
                        .description("Tube station arrivals")
                        .build()
                )
                .schema(ImmutableAttributeSchema.builder().build())
                .features(features)
                .build();
    }
}
