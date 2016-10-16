package io.quartic.weyl.core;

import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Optional;

public class LayerStore extends AbstractLayerStore {

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        super(featureStore, lidGenerator);
    }

    public Optional<IndexedLayer> bucket(BucketSpec bucketSpec) {
        Optional<IndexedLayer> layer = BucketOp.create(this, bucketSpec).map((layer1) -> index(lidGenerator.get(), layer1, (g, f) -> f.stream()));
        layer.ifPresent(this::putLayer);
        return layer;
    }
}
