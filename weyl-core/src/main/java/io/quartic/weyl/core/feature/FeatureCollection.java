package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterators.unmodifiableIterator;
import static com.google.common.collect.Lists.newArrayList;

public class FeatureCollection extends AbstractCollection<Feature> {
    public interface Store {
        void addAll(Collection<Feature> features);
    }

    private final Store store;
    private final List<Feature> features;

    FeatureCollection(Store store) {
        this(store, ImmutableList.of());
    }

    private FeatureCollection(Store store, List<Feature> features) {
        this.store = store;
        this.features = features;
    }

    public FeatureCollection append(Collection<Feature> features) {
        store.addAll(features);
        List<Feature> concat = newArrayList(this.features);
        concat.addAll(features);
        return new FeatureCollection(store, concat);
    }

    @Override
    public Iterator<Feature> iterator() {
        return unmodifiableIterator(features.iterator());
    }

    @Override
    public int size() {
        return features.size();
    }
}
