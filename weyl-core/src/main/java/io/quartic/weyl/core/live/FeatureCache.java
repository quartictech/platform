package io.quartic.weyl.core.live;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.stream.Collectors;

class FeatureCache extends AbstractCollection<Feature<Geometry>> {
    private final Multimap<String, Feature<Geometry>> features = ArrayListMultimap.create();

    @Override
    public synchronized Iterator<Feature<Geometry>> iterator() {
        return features.asMap().entrySet()
                .stream()
                .map(e -> Iterables.getLast(e.getValue()))
                .collect(Collectors.toList())   // We make an explicit copy to avoid concurrency issues
                .iterator();
    }

    @Override
    public synchronized int size() {
        return features.keySet().size();
    }

    @Override
    public synchronized boolean add(Feature<Geometry> feature) {
        return features.put(feature.id(), feature);
    }
}
