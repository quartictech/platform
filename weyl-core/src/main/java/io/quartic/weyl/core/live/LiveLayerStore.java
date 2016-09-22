package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.AbstractFeatureCollection;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Point;

import java.util.Optional;

public class LiveLayerStore {

    public static final FeatureCollection AK_14012159 = FeatureCollection.of(ImmutableList.of(
            Feature.of(
                    Optional.of("ak14012159"),
                    Point.of(ImmutableList.of(-0.10, 51.4800)),
                    ImmutableMap.of()
            )
    ));

    // TODO: use IndexedLayer?
    public Optional<AbstractFeatureCollection> getFeaturesForLayer(String layerId) {
        return Optional.of(AK_14012159);
    }
}
