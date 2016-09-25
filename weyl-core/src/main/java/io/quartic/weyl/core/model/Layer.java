package io.quartic.weyl.core.model;

import java.util.Collection;

public interface Layer {
    AttributeSchema schema();
    AbstractLayerMetadata metadata();
    Collection<Feature> features();
}
