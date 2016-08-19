package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public abstract class BucketSum implements BucketAggregation {
    abstract String property();


    @Override
    public double aggregate(Collection<Feature> features) {
        return features.stream().mapToDouble(feature -> (double) feature.metadata().get(property()))
                .sum();
    }
}
