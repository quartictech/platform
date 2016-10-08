package io.quartic.weyl.core.feature;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.Collection;
import java.util.Map;

public interface FeatureMap extends Map<FeatureId, Feature> {
    public Feature put(Feature feature);
    public void putAll(Collection<? extends Feature> features);
}
