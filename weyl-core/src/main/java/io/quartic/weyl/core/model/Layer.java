package io.quartic.weyl.core.model;

import java.util.Collection;

public interface Layer<T> {
    AttributeSchema schema();
    LayerMetadata metadata();
    Collection<Feature<T>> features();
}
