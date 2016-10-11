package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;

public class FeatureCollection extends AbstractCollection<Feature> {
    public interface Store {
        void addAll(Collection<Feature> features);
    }

    private final Store store;
    private final Iterable<Feature> features;
    private final int size;

    FeatureCollection(Store store) {
        this(store, ImmutableList.of(), 0);
    }

    private FeatureCollection(Store store, Iterable<Feature> features, int size) {
        this.store = store;
        this.features = features;
        this.size = size;
    }

    public FeatureCollection append(List<Feature> features) {
        store.addAll(features);
        return new FeatureCollection(store, concat(this, copyOf(features)), size + features.size());
    }

    @Override
    public Iterator<Feature> iterator() {
        return features.iterator();
    }

    @Override
    public int size() {
        return size;
    }
}
