package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterables.concat;

public class FeatureCollection extends AbstractCollection<Feature> {
    public interface Store {
        void addAll(Collection<Feature> features);
    }

    private final Store store;
    private final List<Feature> features;
    private final Iterable<Feature> prev;
    private final int size;

    FeatureCollection(Store store) {
        this(store, 0, ImmutableList.of(), ImmutableList.of());
    }

    private FeatureCollection(Store store, int prevSize, Iterable<Feature> prev, List<Feature> features) {
        this.store = store;
        this.prev = prev;
        this.features = ImmutableList.copyOf(features);
        this.size = prevSize + features.size();
    }

    public FeatureCollection append(List<Feature> features) {
        store.addAll(features);
        return new FeatureCollection(store, size, this, features);
    }

    @Override
    public Iterator<Feature> iterator() {
        return concat(features, prev).iterator();
    }

    @Override
    public int size() {
        return size;
    }
}
