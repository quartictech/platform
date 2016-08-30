package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Optional;

@Value.Immutable
public abstract class BucketSum implements BucketAggregation {
    abstract String property();


    @Override
    public double aggregate(Collection<Feature> features) {
        return features.stream().map(feature -> feature.metadata().get(property()))
                .filter(Optional::isPresent)
                .mapToDouble( value -> (Double) value.get())
                .sum();
    }
}
