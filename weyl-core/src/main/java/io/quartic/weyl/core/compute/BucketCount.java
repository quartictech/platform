package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public class BucketCount implements BucketAggregation {
    @Override
    public double aggregate(Collection<Feature> features) {
        return features.size();
    }
}
