package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.AbstractFeature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.reverse;

public class FeatureCollection extends AbstractCollection<AbstractFeature> {
    public static final FeatureCollection EMPTY_COLLECTION = new FeatureCollection();

    private final FeatureCollection prev;
    private final List<AbstractFeature> features;
    private final int size;

    FeatureCollection() {
        this(null, ImmutableList.of(), 0);
    }

    private FeatureCollection(FeatureCollection prev, List<AbstractFeature> features, int size) {
        this.prev = prev;
        this.features = features;
        this.size = size;
    }

    public FeatureCollection append(Collection<? extends AbstractFeature> features) {
        return new FeatureCollection(this, reverse(copyOf(features)), size + features.size());
    }

    @Override
    public Iterator<AbstractFeature> iterator() {
        return new Iterator<AbstractFeature>() {
            FeatureCollection collection = FeatureCollection.this;
            Iterator<AbstractFeature> iterator = seedIterator();

            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public AbstractFeature next() {
                final AbstractFeature feature = iterator.next();
                advance();
                return feature;
            }

            private void advance() {
                while (!iterator.hasNext() && collection.prev != null) {
                    collection = collection.prev;
                    iterator = seedIterator();
                }
            }

            private Iterator<AbstractFeature> seedIterator() {
                return collection.features.iterator();
            }
        };
    }

    @Override
    public int size() {
        return size;
    }
}
