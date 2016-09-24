package io.quartic.weyl.core.live;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

class FeatureCache extends AbstractCollection<Feature<Geometry>> {
    Cache<String, Feature<Geometry>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    @Override
    public Iterator<Feature<Geometry>> iterator() {
        return cache.asMap().values().iterator();
    }

    @Override
    public int size() {
        return (int) cache.size();
    }

    @Override
    public boolean add(Feature<Geometry> feature) {
        cache.put(feature.id(), feature);
        return true;
    }
}
