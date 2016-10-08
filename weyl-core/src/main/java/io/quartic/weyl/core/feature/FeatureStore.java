package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

public class FeatureStore extends AbstractMap<FeatureId, Feature> {
    private final Map<FeatureId, Feature> allFeatures = Maps.newHashMap();
    private final UidGenerator<FeatureId> fidGenerator;

    public FeatureStore(UidGenerator<FeatureId> fidGenerator) {
        this.fidGenerator = fidGenerator;
    }

    public UidGenerator<FeatureId> getFeatureIdGenerator() {
        return fidGenerator;
    }

    public Collection<Feature> createImmutableCollection(Collection<? extends Feature> features) {
        features.forEach(this::add);
        return ImmutableList.copyOf(features);
    }

    public Collection<Feature> createMutableCollection() {
        return createMutableCollection(newArrayList());
    }

    public Collection<Feature> createMutableCollection(Collection<? extends Feature> features) {
        features.forEach(this::add);
        return new AbstractCollection<Feature>() {
            private final List<Feature> internal = newArrayList(features);

            @Override
            public Iterator<Feature> iterator() {
                return internal.iterator();
            }

            @Override
            public int size() {
                return internal.size();
            }

            @Override
            public boolean add(Feature feature) {
                final boolean success = internal.add(feature);
                if (success) {
                    FeatureStore.this.add(feature);
                }
                return success;
            }
        };
    }

    private void add(Feature feature) {
        checkArgument(!allFeatures.containsKey(feature.uid()), "Duplicate feature UID");
        allFeatures.put(feature.uid(), feature);
    }

    @Override
    public Set<Entry<FeatureId, Feature>> entrySet() {
        return allFeatures.entrySet();
    }
}
