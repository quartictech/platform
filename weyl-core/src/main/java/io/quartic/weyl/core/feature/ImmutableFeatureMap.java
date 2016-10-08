package io.quartic.weyl.core.feature;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

public class ImmutableFeatureMap extends AbstractMap<FeatureId, Feature> implements FeatureMap {
    private final Map<FeatureId, Feature> internal;

    public ImmutableFeatureMap() {
        internal = Maps.newHashMap();
    }

    public ImmutableFeatureMap(Collection<? extends Feature> features) {
        internal = features.stream().collect(toMap(Feature::uid, v -> v));
    }

    @Override
    public Set<Entry<FeatureId, Feature>> entrySet() {
        return internal.entrySet();
    }

    @Override
    public Feature put(Feature feature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Collection<? extends Feature> features) {
        throw new UnsupportedOperationException();
    }
}
