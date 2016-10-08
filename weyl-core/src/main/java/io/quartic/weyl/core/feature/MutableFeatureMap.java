package io.quartic.weyl.core.feature;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MutableFeatureMap extends AbstractMap<FeatureId, Feature> implements FeatureMap {
    private final Map<FeatureId, Feature> internal;

    public MutableFeatureMap() {
        internal = Maps.newHashMap();
    }

    public MutableFeatureMap(Collection<? extends Feature> features) {
        internal = toMap(features);
    }

    @Override
    public Set<Entry<FeatureId, Feature>> entrySet() {
        return internal.entrySet();
    }

    @Override
    public Feature put(FeatureId key, Feature value) {
        return internal.put(key, value);
    }

    @Override
    public Feature put(Feature feature) {
        return internal.put(feature.uid(), feature);
    }

    @Override
    public void putAll(Collection<? extends Feature> features) {
        internal.putAll(toMap(features));
    }

    private Map<FeatureId, Feature> toMap(Collection<? extends Feature> features) {
        return features.stream().collect(Collectors.toMap(Feature::uid, v -> v));
    }
}
