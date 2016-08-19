package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public interface BucketAggregation {
    double aggregate(Collection<Feature> features);
}
