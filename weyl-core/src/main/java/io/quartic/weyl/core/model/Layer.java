package io.quartic.weyl.core.model;

import java.util.Collection;

public interface Layer<T> {
    AttributeSchema schema();
    AbstractLayerMetadata metadata();
    Collection<Feature<T>> features();
}
