package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;

import java.util.Optional;

public class LiveLayerStore {
    public static final double CENTRE_LNG = -0.10;
    public static final double CENTRE_LAT = 51.4800;
    public static final double RADIUS = 0.1;

    // TODO: use IndexedLayer?
    public Optional<AbstractFeatureCollection> getFeaturesForLayer(String layerId) {
        long time = System.currentTimeMillis();

        final double lng = CENTRE_LNG + RADIUS * Math.cos(2 * Math.PI * time / 10_000);
        final double lat = CENTRE_LAT + RADIUS * Math.sin(2 * Math.PI * time / 10_000);


        return Optional.of(FeatureCollection.of(ImmutableList.of(
                Feature.of(
                        Optional.of("ak14012159"),
                        Point.of(ImmutableList.of(lng, lat)),
                        ImmutableMap.of()
                )
        )));
    }
}
