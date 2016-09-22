package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.model.*;

import java.util.Collection;
import java.util.Optional;

public class LiveLayerStore {
    public static final double CENTRE_LNG = -0.10;
    public static final double CENTRE_LAT = 51.4800;
    public static final double RADIUS = 0.1;

    public Optional<AbstractFeatureCollection> getFeaturesForLayer(String layerId) {
        long time = System.currentTimeMillis();
        final int magic = 1;

        final double radius = RADIUS / magic;
        final double lng = CENTRE_LNG + radius * Math.cos(2 * Math.PI * time / (10_000 * magic));
        final double lat = CENTRE_LAT + radius * Math.sin(2 * Math.PI * time / (10_000 * magic));


        return Optional.of(FeatureCollection.of(ImmutableList.of(
                Feature.of(
                        Optional.of("ak14012159"),
                        Point.of(ImmutableList.of(lng, lat)),
                        ImmutableMap.of("pet names", 5)
                )
        )));
    }

    public Collection<LiveLayer> listLayers() {
        return ImmutableList.of(createFakeLayer());
    }

    private LiveLayer createFakeLayer() {
        return ImmutableLiveLayer.builder()
                .layerId(ImmutableLayerId.of("1234"))
                .layer(ImmutableRawLayer.builder()
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
                        .features(ImmutableList.of())
                        .build()
                        )
                .build();
    }
}
