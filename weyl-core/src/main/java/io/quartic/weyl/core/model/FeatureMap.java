package io.quartic.weyl.core.model;

import java.util.Collection;
import java.util.Map;

public interface FeatureMap extends Map<FeatureId, Feature> {
    public Feature put(Feature feature);
    public void putAll(Collection<? extends Feature> features);
}
