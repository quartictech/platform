package io.quartic.weyl.core.feature;

import com.google.common.collect.MapMaker;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.common.uid.UidGenerator;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;

public class FeatureStore extends AbstractMap<FeatureId, Feature> {
    private final Map<FeatureId, Feature> features = new MapMaker().weakValues().makeMap();
    private final UidGenerator<FeatureId> fidGenerator;
    private final FeatureCollection emptyCollection = new FeatureCollection(this::addFeatures);

    public FeatureStore(UidGenerator<FeatureId> fidGenerator) {
        this.fidGenerator = fidGenerator;
    }

    public UidGenerator<FeatureId> getFeatureIdGenerator() {
        return fidGenerator;
    }

    public FeatureCollection newCollection() {
        return emptyCollection;
    }

    private void addFeatures(Collection<? extends Feature> features) {
        features.forEach(f -> FeatureStore.this.features.put(f.uid(), f));
    }

    @Override
    public Set<Entry<FeatureId, Feature>> entrySet() {
        return unmodifiableMap(features).entrySet();
    }

    @Override
    public boolean containsValue(Object value) {
        return features.containsValue(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return features.containsKey(key);
    }

    @Override
    public Feature get(Object key) {
        return features.get(key);
    }
}
