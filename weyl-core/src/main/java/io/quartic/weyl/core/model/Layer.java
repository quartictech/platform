package io.quartic.weyl.core.model;

public interface Layer {
    AttributeSchema schema();
    LayerMetadata metadata();
    FeatureMap features();
}
