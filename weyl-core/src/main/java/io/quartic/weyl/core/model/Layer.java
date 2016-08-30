package io.quartic.weyl.core.model;

import java.util.Collection;

public interface Layer {
    LayerMetadata metadata();
    Collection<Feature> features();
}
