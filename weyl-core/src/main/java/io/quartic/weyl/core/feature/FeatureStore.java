package io.quartic.weyl.core.feature;

import com.google.common.collect.MapMaker;
import io.quartic.common.uid.UidGenerator;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.FeatureId;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;

public class FeatureStore extends AbstractMap<FeatureId, AbstractFeature> {
    private final Map<FeatureId, AbstractFeature> features = new MapMaker().weakValues().makeMap();
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

    private void addFeatures(Collection<? extends AbstractFeature> features) {
        features.forEach(f -> FeatureStore.this.features.put(f.uid(), f));
    }

    @Override
    public Set<Entry<FeatureId, AbstractFeature>> entrySet() {
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
    public AbstractFeature get(Object key) {
        return features.get(key);
    }
}
