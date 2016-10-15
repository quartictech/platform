package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.Feature;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.reverse;

public class FeatureCollection extends AbstractCollection<Feature> {
    private final Consumer<Collection<? extends Feature>> backer;
    private final FeatureCollection prev;
    private final List<Feature> features;
    private final int size;

    FeatureCollection(Consumer<Collection<? extends Feature>> backer) {
        this(backer, null, ImmutableList.of(), 0);
    }

    private FeatureCollection(Consumer<Collection<? extends Feature>> backer, FeatureCollection prev, List<Feature> features, int size) {
        this.backer = backer;
        this.prev = prev;
        this.features = features;
        this.size = size;
    }

    public FeatureCollection append(Collection<? extends Feature> features) {
        backer.accept(features);
        return new FeatureCollection(backer, this, reverse(copyOf(features)), size + features.size());
    }

    @Override
    public Iterator<Feature> iterator() {
        return new Iterator<Feature>() {
            FeatureCollection collection = FeatureCollection.this;
            Iterator<Feature> iterator = seedIterator();

            {
                advance();
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Feature next() {
                final Feature feature = iterator.next();
                advance();
                return feature;
            }

            private void advance() {
                while (!iterator.hasNext() && collection.prev != null) {
                    collection = collection.prev;
                    iterator = seedIterator();
                }
            }

            private Iterator<Feature> seedIterator() {
                return collection.features.iterator();
            }
        };
    }

    @Override
    public int size() {
        return size;
    }
}
