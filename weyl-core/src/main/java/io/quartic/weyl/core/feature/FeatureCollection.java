package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public class FeatureCollection extends AbstractCollection<Feature> {
    public interface Store {
        void addAll(Collection<Feature> features);
    }

    private final Store store;
    private final FeatureCollection prev;
    private final Iterable<Feature> features;
    private final int size;

    FeatureCollection(Store store) {
        this(store, null, ImmutableList.of(), 0);
    }

    private FeatureCollection(Store store, FeatureCollection prev, Iterable<Feature> features, int size) {
        this.store = store;
        this.prev = prev;
        this.features = features;
        this.size = size;
    }

    public FeatureCollection append(List<Feature> features) {
        store.addAll(features);
        return new FeatureCollection(store, this, copyOf(features), size + features.size());
    }

    @Override
    public Iterator<Feature> iterator() {
        return new Iterator<Feature>() {
            FeatureCollection current = FeatureCollection.this;
            Iterator<Feature> currentIterator = current.features.iterator();

            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return (current != null);
            }

            @Override
            public Feature next() {
                final Feature feature = currentIterator.next();
                advance();
                return feature;
            }

            private void advance() {
                while (true) {
                    if (currentIterator.hasNext()) {
                        return;
                    }
                    current = current.prev;
                    if (current == null) {
                        return;
                    }
                    currentIterator = current.features.iterator();
                }
            }
        };
    }

    @Override
    public int size() {
        return size;
    }
}
