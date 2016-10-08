package io.quartic.weyl.core.model;

import io.quartic.weyl.core.feature.FeatureMap;

public interface Layer {
    AttributeSchema schema();
    LayerMetadata metadata();
    FeatureMap features();
}
