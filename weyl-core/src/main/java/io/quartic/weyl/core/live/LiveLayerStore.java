package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.geojson.*;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LiveLayerStore {
    private final Map<LayerId, Layer<Geometry>> layers = Maps.newHashMap();

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

    private Layer<Geometry> createFakeLayer() {
        return ImmutableRawLayer.<Geometry>builder()
                .metadata(ImmutableLayerMetadata.builder()
                        .name("Weirdness")
                        .description("This is absolute gash")
                        .build()
                )
                .schema(ImmutableAttributeSchema.builder()
                        .putAttributes("pet name", ImmutableAttribute.builder()
                                .type(AttributeType.STRING)
                                .build()
                        )
                        .build()
                )
                .features(ImmutableList.of(
                        new FakeFeature("abcd", 1, ImmutableMap.of()),
                        new FakeFeature("efgh", 2, ImmutableMap.of())
                ))
                .build();
    }
}
